// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.util

import java.io.{InputStream, File}
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.security.{KeyPair, KeyPairGenerator, SecureRandom}
import java.util.Date
import javax.mail.internet.InternetAddress

import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.{X500Name, X500NameBuilder}
import org.bouncycastle.cert.X509v1CertificateBuilder
import org.bouncycastle.cert.jcajce.{JcaX509CertificateConverter, JcaX509v1CertificateBuilder}
import org.bouncycastle.openssl.jcajce.JcaPKIXIdentityBuilder
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity

/** Utilities for generating and loading JcaPKIXIdentity */
object IdentityUtil {
  val providerName: String = "BC"

  /**
   * Loads an identity from key and cert files.
   * todo: test. this will get exercised when we have a permanent dst cert
   */
  def loadIdentity(keyFileName: String, certFileName: String): JcaPKIXIdentity = {
    val keyIs: InputStream = getClass.getClassLoader.getResourceAsStream(keyFileName)
    val certIs: InputStream = getClass.getClassLoader.getResourceAsStream(certFileName)

    new JcaPKIXIdentityBuilder().setProvider(providerName).build(keyIs, certIs)
  }

  /** Generates an JcaPKIXIdentity based on email information. */
  def generateIdentity(address: InternetAddress): JcaPKIXIdentity = generateIdentity(address.getPersonal, address.getAddress)

  /** Generates an JcaPKIXIdentity based on email information. */
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
