// Copyright (C) 2017 Grier Forensics. All Rights Reserved.

package com.grierforensics.danesmimeatoolset

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.service.{EmailSender, MessageDetails}
import com.grierforensics.danesmimeatoolset.util.DstTestValues._
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.cert.dane.DANEEntry
import org.scalatest._

class DaneSmimeaServiceTests extends FunSuite with BeforeAndAfterAll with LazyLogging {

  val testDss = new TestDaneSmimeaService

  test("fetch DANE Entry and create") {
    val de1: DANEEntry = testDss.fetchDaneEntries(emailWithDane).head
    val zl1: String = testDss.getDnsZoneLineForDaneEntry(de1)
    assert(zl1 == emailWithDanePublishedZoneLine)

    val de2: DANEEntry = testDss.createDANEEntry(emailWithDane, testDss.getCertFromDANEEntry(de1))
    val zl2: String = testDss.getDnsZoneLineForDaneEntry(de2)
    assert(zl2 == emailWithDanePublishedZoneLine)
  }

  test("send and fetch email with signing and encryption") {
    val subject: String = "test email: " + System.currentTimeMillis()
    val text: String = "secret message"
    val email = Email(bobAddress, dstAddress, subject, text)

    val encrypted: Email = testDss.signAndEncrypt(email, bobIdentity, dstIdentity.getX509Certificate)
    val encryptedMessage = EmailSender.createMessage(encrypted)

    val details: MessageDetails = testDss.inspectMessage(encryptedMessage, bobIdentity.getX509Certificate, testIdentity)
    assert(details.encrypted)
  }
}







