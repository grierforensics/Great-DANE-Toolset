
package com.grierforensics.danesmimeatoolset

import javax.mail.Message
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.service.{EmailFetcher, EmailSender, MessageDetails}
import com.grierforensics.danesmimeatoolset.util.DstTestValues._
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity
import org.scalatest._

import scala.collection.mutable.ListBuffer

class EmailTests extends FunSuite with BeforeAndAfterAll with LazyLogging {

  test("send and fetch email with signing and encryption") {
    val sender = EmailSender
    val fetcher = new EmailFetcher("pop.gmail.com", "dst.bob@example.com", "dst.bob!")

    val subject: String = "test email: " + System.currentTimeMillis()
    val text: String = "secret message"
    val email = Email(testAddress, bobAddress, subject, text)

    sender.send(email)
    Thread.sleep(1000)
    sender.send(testDss.sign(email, testIdentity))
    Thread.sleep(1000)
    sender.send(testDss.signAndEncrypt(email, testIdentity))
    Thread.sleep(1000)

    val fetched = ListBuffer[MessageDetails]()
    def handle(message: Message): Boolean = {
      if (message.getSubject != subject)
        return false

      fetched += testDss.inspectMessage(message, bobIdentity, testIdentity.getX509Certificate)
      true
    }
    fetcher.fetchAndDelete(handle)

    assert(fetched.size == 3, s"only found ${fetched.size} of 3 emails")

    assert(!fetched(0).encrypted)
    assert(!fetched(0).signingInfo.signatureValid)
    assert(!fetched(0).signingInfo.signedByCert)
    assert(fetched(0).text == Some(text))

    assert(!fetched(1).encrypted)
    assert(fetched(1).signingInfo.signatureValid)
    assert(fetched(1).signingInfo.signedByCert)
    assert(fetched(1).text == Some(text))

    assert(fetched(2).encrypted)
    assert(fetched(2).signingInfo.signatureValid)
    assert(fetched(2).signingInfo.signedByCert)
    assert(fetched(2).text == Some(text))
  }


  test("email async service") {
    val sender = EmailSender
    val fetcher = EmailFetcher

    val subject: String = "test email: " + System.currentTimeMillis();
    val text: String = "secret message"
    val email = Email(testAddress, fetcher.address, subject, text)

    val fetched = ListBuffer[MessageDetails]()
    def handler(message: Message): Boolean = {
      if (message.getSubject != subject)
        return false

      fetched += testDss.inspectMessage(message, bobIdentity, testIdentity.getX509Certificate)
      true
    }

    //async with no sends
    fetcher.asyncFetchAndDelete(handler)
    assert(fetcher.asyncIsFetching)
    fetcher.waitForFetch()
    assert(!fetcher.asyncIsFetching)
    assert(fetched.size == 0)

    //async with sends
    sender.send(email)
    fetcher.asyncFetchAndDelete(handler)
    assert(fetched.size == 0)
    fetcher.waitForFetch(2)
    assert(fetched.size == 1)

    //async periodic
    fetched.clear()
    fetcher.asyncFetchAndDelete(handler, 1000)
    assert(fetcher.asyncIsFetching)
    sender.send(email)
    assert(fetched.size == 0)
    fetcher.waitForFetch(2)
    assert(fetched.size == 1)
    sender.send(email)
    fetcher.waitForFetch(2)
    assert(fetched.size == 2)
    assert(fetcher.asyncIsFetching)
    fetcher.asyncStop()
    assert(!fetcher.asyncIsFetching)
  }
}







