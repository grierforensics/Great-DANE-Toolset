// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset

import java.security.cert.X509Certificate

import com.grierforensics.danesmimeatoolset.service.DaneSmimeaService
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity

/** DaneSmimeService that allows overriding of fetchDaneCert results for given emailAddress's. */
class TestDaneSmimeaService extends DaneSmimeaService(config.getString("DaneSmimeaService.dns")) {

  val daneCertOverrides = collection.mutable.HashMap[String, X509Certificate]()

  override def fetchDaneCert(emailAddress: String): Option[X509Certificate] = {
    daneCertOverrides.get(emailAddress) match {
      case None => super.fetchDaneCert(emailAddress)
      case some => some
    }
  }


  def overrideDaneCert(emailAddress: String, cert: X509Certificate) = {
    daneCertOverrides.put(emailAddress, cert)
  }

}

