package com.grierforensics.danesmimeatoolset

import java.security.cert.X509Certificate

import com.grierforensics.danesmimeatoolset.service.DaneSmimeService
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity

/**
 * DaneSmimeService that generates identities when the DANE SMIME cert is unavailable.
 */
class TestDaneSmimeService extends DaneSmimeService(config.getString("DaneSmimeService.dns")) {

  val identities = collection.mutable.HashMap[String, JcaPKIXIdentity]()

  override def fetchCert(emailAddress: String): Option[X509Certificate] = {
    super.fetchCert(emailAddress) match {
      case None => {
        val ident: JcaPKIXIdentity = identities.getOrElse(emailAddress, generateIdentity("Fake Identity", emailAddress))
        Option(ident.getX509Certificate)
      }
      case identOption => identOption
    }
  }

  override def generateIdentity(fullName: String, emailAddress: String): JcaPKIXIdentity = {
    val result: JcaPKIXIdentity = super.generateIdentity(fullName, emailAddress)
    identities += emailAddress -> result
    result
  }
}

