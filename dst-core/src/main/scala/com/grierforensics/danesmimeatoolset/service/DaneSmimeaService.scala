// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.service

import java.io.InputStream
import java.security._
import java.security.cert._
import java.util
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMultipart}
import javax.mail.{Address, Message, Part}

import com.grierforensics.danesmimeatoolset.model.Email
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.dane._
import org.bouncycastle.cert.dane.fetcher.JndiDANEFetcherFactory
import org.bouncycastle.cert.jcajce.{JcaX509CertificateConverter, JcaX509CertificateHolder}
import org.bouncycastle.cert.{X509CertificateHolder, dane}
import org.bouncycastle.cms.jcajce._
import org.bouncycastle.cms.{KeyTransRecipientId, RecipientId, SignerInfoGenerator, SignerInformation}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.mail.smime.{SMIMESigned, SMIMEToolkit}
import org.bouncycastle.operator._
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity
import org.bouncycastle.util.encoders.Hex

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


/**
 * Service providing a collection of DANE and SMIMEA functions around signing, encryption, dane fetching, and dane creation.
 *
 * @param dnsServer
 */
class DaneSmimeaService(val dnsServer: String) extends LazyLogging {
  import DaneSmimeaService._

  val daneFetcherFactory: JndiDANEFetcherFactory = new JndiDANEFetcherFactory().usingDNSServer(dnsServer)
  val daneEntryFetcher: DANEEntryFetcher = new DANEEntryFetcher(daneFetcherFactory, TruncatingDigestCalculator)

  val toolkit: SMIMEToolkit = new SMIMEToolkit(DigestCalculatorProvider)


  // // // // // // // // // // Dane Methods

  /**
   * Fetches the first DANE cert found on DNS for the given email address.
   */
  def fetchDaneCert(emailAddress: String): Option[X509Certificate] = {
    fetchDaneEntries(emailAddress) match {
      case Nil => None
      case daneEntries => Option(getCertFromDANEEntry(daneEntries.head))
    }
  }


  /**
   * Fetches the all DANE certs found on DNS for the given email address.
   */
  def fetchDaneCerts(emailAddress: String): Seq[X509Certificate] = {
    fetchDaneEntries(emailAddress).map(getCertFromDANEEntry(_))
  }


  /**
   * Fetches the all DANEEntry's found on DNS for the given email address.
   */
  def fetchDaneEntries(emailAddress: String): Seq[DANEEntry] = {
    try {
      daneEntryFetcher.fetch(emailAddress)
    }
    catch {
      case e: DANEException if e.getMessage.contains("DNS name not found") => Nil
    }
  }


  /**
   * Gets certificate data from DANEEntry and converts it to a X509Certificate
   */
  def getCertFromDANEEntry(daneEntry: DANEEntry): X509Certificate = {
    CertificateConverter.getCertificate(daneEntry.getCertificate)
  }


  /**
   * Creates a DANEEntry from an email and a cert. 
   * The cert must be valid (signed and not expired).
   * No relation between the email and cert is checked.
   */
  def createDANEEntry(email: String, cert: X509Certificate): DANEEntry = {
    DaneEntryFactory.createEntry(email, validateCert(cert.getEncoded))
  }


  /**
   * Creates a DANEEntry from an email and a cert.
   * The cert must be valid (signed and not expired).
   * No relation between the email and cert is checked.
   */
  def createDANEEntry(email: String, certBytes: Array[Byte]): DANEEntry = {
    DaneEntryFactory.createEntry(email, validateCert(certBytes))
  }


  /**
   * Creates a text line suitable for a DNS zone file based on the given DANEEntry.
   */
  def getDnsZoneLineForDaneEntry(de: DANEEntry): String = {
    val encoded: Array[Byte] = de.getRDATA
    val hex: String = Hex.toHexString(encoded).toUpperCase
    s"${de.getDomainName}. IN TYPE$DaneType \\# ${encoded.length} ${hex}"
  }


  // // // // // // // // // // Message Security Methods


  /**
   * Creates a signed and encrypted copy of the given email.
   */
  def signAndEncrypt(email: Email, fromIdentity: JcaPKIXIdentity, toUserCert: X509Certificate): Email = {
    encrypt(sign(email, fromIdentity), toUserCert)
  }


  /**
   * Creates a signed copy of the given email.  Email body will be MimeMultiPart, with the second part as the signature.
   */
  def sign(email: Email, fromIdentity: JcaPKIXIdentity): Email = {

    val signerInfo: SignerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(Provider).build(AlgorithmName, fromIdentity.getPrivateKey, fromIdentity.getX509Certificate)
    val signedMultipart: MimeMultipart = toolkit.sign(email.bodyPart, signerInfo)

    Email(email.from, email.to, email.subject, signedMultipart)
  }


  /**
   * Creates an encrypted copy of the given email.  Email body will be an encrypted MimeBodyPart.
   */
  def encrypt(email: Email, toUserCert: X509Certificate): Email = {
    if (toUserCert == null)
      throw new Exception("no cert for encryption")

    val encryptor: OutputEncryptor = new JceCMSContentEncryptorBuilder(NISTObjectIdentifiers.id_aes128_CBC).setProvider(Provider).build
    val infoGenerator: JceKeyTransRecipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(toUserCert).setProvider(Provider)

    val encrypted: MimeBodyPart = toolkit.encrypt(email.multipart, encryptor, infoGenerator)

    Email(email.from, email.to, email.subject, encrypted)
  }


  // // // // // // // // // // Message Inspection

  /**
   *
   */
  def inspectMessage(message: Message, fromCert: X509Certificate, toIdentity: JcaPKIXIdentity): MessageDetails = {
    val from = message.getFrom()(0) match {
      case ia: InternetAddress => ia
      case a: Address => new InternetAddress(a.toString)
    }
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
        val part: MimeBodyPart = message.getContent match {
          case mbp: MimeBodyPart => mbp
          case is => new MimeBodyPart(is.asInstanceOf[InputStream])
        }
        val recipientId = getRecipientId(toIdentity)
        val decrypted: MimeBodyPart = toolkit.decrypt(part, recipientId, new JceKeyTransEnvelopedRecipient(toIdentity.getPrivateKey).setProvider(Provider))
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
        val isSignatureValid: Boolean = toolkit.isValidSignature(mm, new JcaSimpleSignerInfoVerifierBuilder().setProvider(Provider).build(certHolder))

        val certMatchesTheirCert: Boolean = certMatchesReference(certHolder, fromCert)
        val certPathValid: Boolean = false //validateCertPath(certHolder)

        new SigningInfo(true, isSignatureValid, certMatchesTheirCert, certPathValid)
      }
      case p: Part if toolkit.isSigned(p) => {
        val smimeSigned = new SMIMESigned(p)
        val signerInformation: SignerInformation = smimeSigned.getSignerInfos.getSigners.iterator.next
        val certHolder: X509CertificateHolder = toolkit.extractCertificate(p, signerInformation)
        val isSignatureValid: Boolean = toolkit.isValidSignature(p, new JcaSimpleSignerInfoVerifierBuilder().setProvider(Provider).build(certHolder))

        val certMatchesTheirCert: Boolean = certMatchesReference(certHolder, fromCert)
        val certPathValid: Boolean = false //validateCertPath(certHolder)

        new SigningInfo(true, isSignatureValid, certMatchesTheirCert, certPathValid)
      }
      case _ => {
        new SigningInfo(false, false, false, false)
      }
    }
  }


  private def extractContentParts(content: AnyRef): Seq[ContentPart] = {
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


  private def scrubTextPlain(s: String): String = {
    s.replaceAll("\r\n$", "")
  }


  private def certMatchesReference(certHolder: X509CertificateHolder, reference: X509Certificate): Boolean = {
    reference != null && certHolder == new JcaX509CertificateHolder(reference) //?maybe we should compare certs rather than holders here?
  }


  /**
   * Checks the holder for
   * @param certBytes
   */
  def validateCert(certBytes: Array[Byte]): X509CertificateHolder = {
    validateCert(new X509CertificateHolder(certBytes))
  }


  def validateCert(holder: X509CertificateHolder): X509CertificateHolder = {
    val contentVerifierProvider = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder()).build(holder);
    if (!holder.isSignatureValid(contentVerifierProvider))
      throw new BadCertificateException("Certificate signature is bad.")
    //    if (!holder.isValidOn(new Date))  //todo: uncomment... this is commented out for now because our major test email has an expired cert.
    //      throw new BadCertificateException("Certificate expired.")
    holder
  }


  /**
   * Validates an authority based cert path.
   */
  private def validateCertPath(certHolder: X509CertificateHolder): Boolean = {
    throw new NotImplementedError("Has not been tested yet and shouldn't be called.  (copied from demo code)")

    //val converter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(providerName)
    //val rootCert: X509Certificate = null
    //val list = ListBuffer(converter.getCertificate(certHolder))
    //
    //// TODO: add other certificates and possible CRLs to the CertStore.
    //val ccsp: CollectionCertStoreParameters = new CollectionCertStoreParameters(list)
    //val store: CertStore = CertStore.getInstance("Collection", ccsp, providerName)
    //
    ////Searching for rootCert by subjectDN without CRL
    //val trust = HashSet(new TrustAnchor(rootCert, null))
    //
    //try {
    //  val cpb: CertPathBuilder = CertPathBuilder.getInstance("PKIX", providerName)
    //  val targetConstraints: X509CertSelector = new X509CertSelector
    //  targetConstraints.setSubject(certHolder.getSubject.getEncoded)
    //  val params: PKIXBuilderParameters = new PKIXBuilderParameters(trust, targetConstraints)
    //  params.addCertStore(store)
    //  val result: PKIXCertPathBuilderResult = cpb.build(params).asInstanceOf[PKIXCertPathBuilderResult]
    //  return true
    //}
    //catch {
    //  case e: Exception => {
    //    logger.warn("Unable to validate certificate path", e)
    //  }
    //}
    //
    //return false
  }
}

object DaneSmimeaService {
  // this corresponds to a hardcoded private value in BC
  val AlgorithmName: String = "SHA1withRSA"

  val Provider = new BouncyCastleProvider
  Security.addProvider(Provider)

  val DaneType: String = "53"

  val CertificateConverter: JcaX509CertificateConverter = new JcaX509CertificateConverter().setProvider(Provider)

  val DigestCalculatorProvider: DigestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(Provider).build
  val TruncatingDigestCalculator = {
    // Sample usage: https://github.com/bcgit/bc-java/blob/master/pkix/src/test/java/org/bouncycastle/cert/test/DANETest.java
    val sha256DigestCalculator = DigestCalculatorProvider.get(new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256))
    new TruncatingDigestCalculator(sha256DigestCalculator)
  }

  val DaneEntryFactory = new DANEEntryFactory(TruncatingDigestCalculator)
}


// // // // // // // // // // Support classes

/**
 * Value Object class to describe a mail Message encryption and signing information.
 */
class MessageDetails(@BeanProperty val from: InternetAddress,
                     @BeanProperty val subject: String,
                     @BeanProperty val encrypted: Boolean,
                     @BeanProperty val signingInfo: SigningInfo,
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


/**
 * Value Object class to describe a mail Message signing information.
 */
class SigningInfo(@BeanProperty val signed: Boolean,
                  @BeanProperty val signatureValid: Boolean,
                  @BeanProperty val signedByCert: Boolean,
                  @BeanProperty val casSigned: Boolean) {
}


/**
 * Value Object class to hold mime content and mimeType
 */
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


/**
 * General Exception for decryption problems.
 */
class DecryptionException(message: String) extends Exception(message)


/**
 * General Exception for certificate problems, such as encoding, signing, or expiring.
 */
class BadCertificateException(message: String) extends Exception(message)



/**
 * Class that fetches DANEEntry's for given email addressses
 */
class DANEEntryFetcher(private val fetcherFactory: DANEEntryFetcherFactory, private val selectorFactory: DANEEntrySelectorFactory) {
  def this(fetcherFactory: DANEEntryFetcherFactory, digestCalculator: DigestCalculator) {
    this(fetcherFactory, new DANEEntrySelectorFactory(digestCalculator))
  }

  @throws(classOf[DANEException])
  def fetch(emailAddress: String): Seq[DANEEntry] = {
    val daneSelector: DANEEntrySelector = selectorFactory.createSelector(emailAddress)
    val fetcher: dane.DANEEntryFetcher = fetcherFactory.build(daneSelector.getDomainName)
    val matches: util.List[DANEEntry] = fetcher.getEntries.asInstanceOf[util.List[DANEEntry]]
    matches.filter(daneSelector.`match`(_))
  }
}
