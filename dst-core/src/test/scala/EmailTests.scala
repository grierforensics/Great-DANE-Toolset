package com.grierforensics.danesmimeatoolset

import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity
import org.scalatest._

class EmailTests extends FunSuite with BeforeAndAfterAll {
  val daneSmimeService = DaneSmimeService

  val email = Email(Some("Alice"), "test1.dst@example.com", Some("Bob"), "test2.dst@example.com", "secret subject", "secret message")
  val fromIdentity: JcaPKIXIdentity = daneSmimeService.generateIdentity(email.fromFullName.getOrElse(email.fromEmailAddress), email.fromEmailAddress)

  test("send email") {
    val sender = new EmailSender("smtp.gmail.com", "test1.dst@example.com", "test1.dst!")

    //smoke test
    sender.send(daneSmimeService.sign(email, fromIdentity)) //todo:update dns so we can do cert download and cal signAndEncrypt
  }

  //todo: implement test("receive email")
}







