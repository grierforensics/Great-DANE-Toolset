package com.grierforensics.danesmimeatoolset

import java.io.File
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.security.{KeyPair, KeyPairGenerator, SecureRandom}
import java.util.Date
import javax.mail.internet.{MimeBodyPart, MimeMultipart}

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.{X500Name, X500NameBuilder}
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.dane.DANECertificateFetcher
import org.bouncycastle.cert.dane.fetcher.JndiDANEFetcherFactory
import org.bouncycastle.cert.jcajce.{JcaX509CertificateConverter, JcaX509v1CertificateBuilder}
import org.bouncycastle.cert.{X509CertificateHolder, X509v1CertificateBuilder}
import org.bouncycastle.cms.SignerInfoGenerator
import org.bouncycastle.cms.jcajce.{JcaSimpleSignerInfoGeneratorBuilder, JceCMSContentEncryptorBuilder, JceKeyTransRecipientInfoGenerator}
import org.bouncycastle.mail.smime.SMIMEToolkit
import org.bouncycastle.openssl.jcajce.JcaPKIXIdentityBuilder
import org.bouncycastle.operator.jcajce.{JcaContentSignerBuilder, JcaDigestCalculatorProviderBuilder}
import org.bouncycastle.operator.{ContentSigner, DigestCalculator, DigestCalculatorProvider, OutputEncryptor}
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity


class DaneSmimeService(val dnsServer: String = "8.8.8.8") {

  val providerName: String = "BC"

  val digestCalculatorProvider: DigestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(providerName).build
  val toolkit: SMIMEToolkit = new SMIMEToolkit(digestCalculatorProvider)


  def signAndEncrypt(email: Email, fromIdentity: JcaPKIXIdentity): Email = {
    encrypt(sign(email, fromIdentity))
  }

  def sign(email: Email, fromIdentity: JcaPKIXIdentity): Email = {

    val signerInfo: SignerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(providerName).build("SHA1withRSA", fromIdentity.getPrivateKey, fromIdentity.getX509Certificate)
    val signedMultipart: MimeMultipart = toolkit.sign(email.bodyPart, signerInfo)

    Email(email.fromFullName, email.fromEmailAddress,
      email.toFullName, email.toEmailAddress,
      email.subject, signedMultipart)
  }

  def encrypt(email: Email): Email = {
    val toUserCert: X509Certificate = fetchCertForEmailAddress(digestCalculatorProvider, email.toEmailAddress)

    val encryptor: OutputEncryptor = new JceCMSContentEncryptorBuilder(NISTObjectIdentifiers.id_aes128_CBC).setProvider(providerName).build
    val infoGenerator: JceKeyTransRecipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(toUserCert).setProvider(providerName)

    val encrypted: MimeBodyPart = toolkit.encrypt(email.multipart, encryptor, infoGenerator)

    Email(email.fromFullName, email.fromEmailAddress,
      email.toFullName, email.toEmailAddress,
      email.subject, encrypted)
  }

  private def fetchCertForEmailAddress(digestCalculatorProvider: DigestCalculatorProvider, toEmailAddress: String): X509Certificate = {
    val sha224Calculator: DigestCalculator = digestCalculatorProvider.get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224))
    val fetcher: JndiDANEFetcherFactory = new JndiDANEFetcherFactory().usingDNSServer(dnsServer)
    val certFetcher: DANECertificateFetcher = new DANECertificateFetcher(fetcher, sha224Calculator)
    val userCertHolder: X509CertificateHolder = certFetcher.fetch(toEmailAddress).get(0).asInstanceOf[X509CertificateHolder]
    val certConverter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(providerName)
    return certConverter.getCertificate(userCertHolder)
  }

  def loadIdentity(keyFileName: String, certFileName: String): JcaPKIXIdentity = {
    val keyFile: File = new File(keyFileName)
    if (!keyFile.canRead) {
      return null
    }
    val certFile: File = new File(certFileName)

    new JcaPKIXIdentityBuilder().setProvider(providerName).build(keyFile, certFile)
  }

  def generateIdentity(fullName: String, emailAddress: String): JcaPKIXIdentity = {
    //The openssl commands used are:
    //# 1.	Create a new RSA private key of 2048 bits:
    //$ openssl genrsa -out certkey.pem 2048
    //# 2.	Create the private key and corresponding public key in DER (binary) form:
    //# $ openssl rsa -in certkey.pem -outform der -out certkey.der
    //# $ openssl rsa -in certkey.pem -pubout -outform der -out certpub.der
    //# 3.	Create a self-signed certificate using that key. The user name here is "Sample Person" and her e-mail address is "sample@example.org":
    //$ openssl req -subj "/CN=Sample Person/emailAddress=sample@example.org" -new -key certkey.pem -x509 -days 3660 -outform der -out cert.der
    val kpGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA", providerName)
    kpGen.initialize(2048, new SecureRandom)

    // after this the keys are created. getEncoded() on the rsaPair.getPrivate() and rsaPair.getPublic() will
    // return the DER encoding of the key.
    val rsaPair: KeyPair = kpGen.generateKeyPair

    // specify the name to go in the subject/issuer for the certificate
    val x500Bldr: X500NameBuilder = new X500NameBuilder

    x500Bldr.addRDN(BCStyle.CN, fullName)
    x500Bldr.addRDN(BCStyle.EmailAddress, emailAddress)

    val id: X500Name = x500Bldr.build

    // set the start and expiry dates for the certificate
    val notBefore: Date = new Date(System.currentTimeMillis - (5 * 60 * 1000))
    val notAfter: Date = new Date(notBefore.getTime + 3660L * 24 * 60 * 60 * 1000)

    // build the certificate
    val certBldr: X509v1CertificateBuilder = new JcaX509v1CertificateBuilder(id, BigInteger.valueOf(System.currentTimeMillis), notBefore, notAfter, id, rsaPair.getPublic)

    val signer: ContentSigner = new JcaContentSignerBuilder("SHA1withRSA").setProvider(providerName).build(rsaPair.getPrivate)
    val certConverter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(providerName)

    // create self-signed certificate
    val rsaCert: X509Certificate = certConverter.getCertificate(certBldr.build(signer))

    new JcaPKIXIdentity(rsaPair.getPrivate, Array[X509Certificate](rsaCert))
  }


}
