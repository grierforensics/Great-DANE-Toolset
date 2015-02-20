package com.grierforensics.danesmimeatoolset.service

import java.io.{File, InputStream}
import java.math.BigInteger
import java.security._
import java.security.cert._
import java.util._
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMultipart}
import javax.mail.{Address, Message, Part}

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.util.ConfigHolder.config
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.{X500Name, X500NameBuilder}
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.dane.fetcher.JndiDANEFetcherFactory
import org.bouncycastle.cert.dane.{DANECertificateFetcher, DANEEntry, DANEEntryFactory, DANEException}
import org.bouncycastle.cert.jcajce.{JcaX509CertificateConverter, JcaX509CertificateHolder, JcaX509v1CertificateBuilder}
import org.bouncycastle.cert.{X509CertificateHolder, X509v1CertificateBuilder}
import org.bouncycastle.cms.jcajce._
import org.bouncycastle.cms.{KeyTransRecipientId, RecipientId, SignerInfoGenerator, SignerInformation}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.mail.smime.{SMIMESigned, SMIMEToolkit}
import org.bouncycastle.openssl.jcajce.JcaPKIXIdentityBuilder
import org.bouncycastle.operator.jcajce.{JcaContentSignerBuilder, JcaDigestCalculatorProviderBuilder}
import org.bouncycastle.operator.{ContentSigner, DigestCalculator, DigestCalculatorProvider, OutputEncryptor}
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity
import org.bouncycastle.util.encoders.Hex

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer


class DaneSmimeService(val dnsServer: String) extends LazyLogging {
  val bouncyCastleProviderSetup = BouncyCastleProviderSetup
  val providerName: String = "BC"
  val digestCalculatorProvider: DigestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(providerName).build
  val sha224Calculator: DigestCalculator = digestCalculatorProvider.get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224))

  val toolkit: SMIMEToolkit = new SMIMEToolkit(digestCalculatorProvider)


  def signAndEncrypt(email: Email, fromIdentity: JcaPKIXIdentity): Email = {
    encrypt(sign(email, fromIdentity))
  }


  def sign(email: Email): Email = sign(email, generateIdentity(email.from))


  def sign(email: Email, fromIdentity: JcaPKIXIdentity): Email = {

    val signerInfo: SignerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(providerName).build("SHA1withRSA", fromIdentity.getPrivateKey, fromIdentity.getX509Certificate)
    val signedMultipart: MimeMultipart = toolkit.sign(email.bodyPart, signerInfo)

    Email(email.from, email.to, email.subject, signedMultipart)
  }


  def fetchCert(emailAddress: String) = fetchCertForEmailAddress(digestCalculatorProvider, emailAddress)


  def getDANEEntryZoneLine(de: DANEEntry): String = {
    val encoded: Array[Byte] = de.getCertificate.getEncoded
    """%s 299 IN TYPE65500 \# %d %s""".format(de.getDomainName, encoded.length, Hex.toHexString(encoded))
  }


  def encrypt(email: Email): Email = encrypt(email, fetchCert(email.to.getAddress).orNull)


  def encrypt(email: Email, toUserCert: X509Certificate): Email = {
    if (toUserCert == null)
      throw new Exception("no cert for encryption")

    val encryptor: OutputEncryptor = new JceCMSContentEncryptorBuilder(NISTObjectIdentifiers.id_aes128_CBC).setProvider(providerName).build
    val infoGenerator: JceKeyTransRecipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(toUserCert).setProvider(providerName)

    val encrypted: MimeBodyPart = toolkit.encrypt(email.multipart, encryptor, infoGenerator)

    Email(email.from, email.to, email.subject, encrypted)
  }


  private def fetchCertForEmailAddress(digestCalculatorProvider: DigestCalculatorProvider, toEmailAddress: String): Option[X509Certificate] = {
    val fetcher: JndiDANEFetcherFactory = new JndiDANEFetcherFactory().usingDNSServer(dnsServer)
    val certFetcher: DANECertificateFetcher = new DANECertificateFetcher(fetcher, sha224Calculator)
    try {
      val userCertHolder: X509CertificateHolder = certFetcher.fetch(toEmailAddress).get(0).asInstanceOf[X509CertificateHolder]
      val certConverter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(providerName)

      Option(certConverter.getCertificate(userCertHolder))
    }
    catch {
      case e: DANEException if e.getMessage.contains("DNS name not found") => None
    }
  }


  def createDANEEntry(email: String, cert: X509Certificate): DANEEntry = {
    createDANEEntry(email, cert.getEncoded)
  }


  def createDANEEntry(email: String, certBytes: Array[Byte]): DANEEntry = {
    val daneEntryFactory = new DANEEntryFactory(sha224Calculator)
    val holder: X509CertificateHolder = new X509CertificateHolder(certBytes)
    val entry: DANEEntry = daneEntryFactory.createEntry(email, holder)
    entry
  }


  def loadIdentity(keyFileName: String, certFileName: String): JcaPKIXIdentity = {
    val keyFile: File = new File(keyFileName)
    if (!keyFile.canRead) {
      return null
    }
    val certFile: File = new File(certFileName)

    new JcaPKIXIdentityBuilder().setProvider(providerName).build(keyFile, certFile)
  }


  def generateIdentity(address: InternetAddress): JcaPKIXIdentity = generateIdentity(address.getPersonal, address.getAddress)

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


  def inspectMessage(message: Message, toIdentity: JcaPKIXIdentity, fromCert: X509Certificate): MessageDetails = {
    val from: Address = message.getFrom()(0)
    val subject: String = message.getSubject()
    val encrypted = toolkit.isEncrypted(message)
    val content: AnyRef = extractContent(message, toIdentity)
    val signingInfo = extractSigningInfo(fromCert, content)
    val contentParts = extractContentParts(content)
    new MessageDetails(from, subject, encrypted, signingInfo, contentParts)
  }


  def extractContent(message: Message, toIdentity: JcaPKIXIdentity): AnyRef = {
    toolkit.isEncrypted(message) match {
      case true => {
        if (toIdentity == null)
          throw new DecryptionException("Private key unavailable for decryption")
        val part: MimeBodyPart = new MimeBodyPart(message.getContent.asInstanceOf[InputStream])
        val recipientId = getRecipientId(toIdentity)
        val decrypted: MimeBodyPart = toolkit.decrypt(part, recipientId, new JceKeyTransEnvelopedRecipient(toIdentity.getPrivateKey).setProvider(providerName))
        if (decrypted.isMimeType("multipart/signed"))
          decrypted.getContent
        else
          decrypted
      }
      case false => message.getContent
    }
  }

  def getRecipientId(toIdentity: JcaPKIXIdentity): RecipientId = {
    try {
      toIdentity.getRecipientId
    }
    catch {
      //hack: bug in BC where certificate extensions aren't handled
      case e: NullPointerException => new KeyTransRecipientId(toIdentity.getCertificate.getIssuer, toIdentity.getCertificate.getSerialNumber, null)
    }
  }

  def extractSigningInfo(fromCert: X509Certificate, content: AnyRef): SigningInfo = {
    content match {
      case mm: MimeMultipart if toolkit.isSigned(mm) => {
        val smimeSigned: SMIMESigned = new SMIMESigned(mm)
        val signerInformation: SignerInformation = smimeSigned.getSignerInfos.getSigners.iterator.next
        val certHolder: X509CertificateHolder = toolkit.extractCertificate(mm, signerInformation)
        val isSignatureValid: Boolean = toolkit.isValidSignature(mm, new JcaSimpleSignerInfoVerifierBuilder().setProvider(providerName).build(certHolder))

        val certMatchesThierCert: Boolean = validateCert(certHolder, fromCert)
        val certPathValid: Boolean = false //validateCertPath(certHolder)

        new SigningInfo(true, isSignatureValid, certMatchesThierCert, certPathValid)
      }
      case p: Part if toolkit.isSigned(p) => {
        val smimeSigned = new SMIMESigned(p)
        val signerInformation: SignerInformation = smimeSigned.getSignerInfos.getSigners.iterator.next
        val certHolder: X509CertificateHolder = toolkit.extractCertificate(p, signerInformation)
        val isSignatureValid: Boolean = toolkit.isValidSignature(p, new JcaSimpleSignerInfoVerifierBuilder().setProvider(providerName).build(certHolder))

        val certMatchesThierCert: Boolean = validateCert(certHolder, fromCert)
        val certPathValid: Boolean = false //validateCertPath(certHolder)

        new SigningInfo(true, isSignatureValid, certMatchesThierCert, certPathValid)
      }
      case _ => {
        new SigningInfo(false, false, false, false)
      }
    }
  }


  def extractContentParts(content: AnyRef): Seq[ContentPart] = {
    content match {
      case mm: MimeMultipart => {
        var result: ListBuffer[ContentPart] = ListBuffer()
        for (i <- 0 until mm.getCount) {
          result ++= extractContentParts(mm.getBodyPart(i))
        }
        result
      }
      case p: Part => ListBuffer(new ContentPart(p.getContentType, p.getContent))
      case s: String => ListBuffer(new ContentPart("text/plain", scrubTextPlain(s)))
      case any => ListBuffer(new ContentPart("unknown", any))
    }
  }


  def scrubTextPlain(s: String): String = {
    s.replaceAll("\r\n$", "")
  }


  def validateCert(certHolder: X509CertificateHolder, theirCertificate: X509Certificate): Boolean = {
    certHolder == new JcaX509CertificateHolder(theirCertificate)
  }


  def validateCertPath(certHolder: X509CertificateHolder): Boolean = {
    val converter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(providerName)
    val rootCert: X509Certificate = null
    val list = ListBuffer(converter.getCertificate(certHolder))

    // TODO: add other certificates and possible CRLs to the CertStore.
    val ccsp: CollectionCertStoreParameters = new CollectionCertStoreParameters(list)
    val store: CertStore = CertStore.getInstance("Collection", ccsp, providerName)

    //Searching for rootCert by subjectDN without CRL
    val trust = HashSet(new TrustAnchor(rootCert, null))

    try {
      val cpb: CertPathBuilder = CertPathBuilder.getInstance("PKIX", providerName)
      val targetConstraints: X509CertSelector = new X509CertSelector
      targetConstraints.setSubject(certHolder.getSubject.getEncoded)
      val params: PKIXBuilderParameters = new PKIXBuilderParameters(trust, targetConstraints)
      params.addCertStore(store)
      val result: PKIXCertPathBuilderResult = cpb.build(params).asInstanceOf[PKIXCertPathBuilderResult]
      return true
    }
    catch {
      case e: Exception => {
        logger.warn("Unable to validate certificate path", e)
      }
    }

    return false
  }
}


class MessageDetails(val from: Address,
                     val subject: String,
                     val encrypted: Boolean,
                     val signingInfo: SigningInfo,
                     val parts: Seq[ContentPart]) {
  def text: Option[String] = {
    parts.find(_.mimeType.startsWith("text")) match {
      case Some(cp) => Option(cp.content.toString)
      case None => None
    }
  }

  override def toString: String = {
    val partsDump = parts.mkString("\n")

    s"""=== $from - $subject
       |--- encrypted:$encrypted signed:${signingInfo.signed} signatureValid:${signingInfo.signatureValid} signedByCert:${signingInfo.signedByCert} casSigned:${signingInfo.casSigned}
       |$partsDump
     """.stripMargin
  }
}

class SigningInfo(val signed: Boolean,
                  val signatureValid: Boolean,
                  val signedByCert: Boolean,
                  val casSigned: Boolean) {
}

class ContentPart(val mimeType: String, val content: AnyRef) {
  override def toString: String = {
    val contentStr = content match {
      //case is: BASE64DecoderStream if mimeType=="application/pkcs7-signature; name=smime.p7s; smime-type=signed-data" => {
      //  val writer = new StringWriter()
      //  IOUtils.copy(is, writer)
      //  writer.toString
      //}
      case everythingElse => everythingElse.toString
    }

    "--- " + mimeType + "\n" + contentStr
  }
}

class DecryptionException(message: String) extends Exception(message)

//// Companion objects

object DaneSmimeService extends DaneSmimeService(config.getString("DaneSmimeService.dns"))

object BouncyCastleProviderSetup {
  Security.addProvider(new BouncyCastleProvider)
}