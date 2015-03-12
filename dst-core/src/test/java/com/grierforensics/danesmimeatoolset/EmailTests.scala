package com.grierforensics.danesmimeatoolset

import javax.mail.Message

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.service.{EmailFetcher, EmailSender, MessageDetails}
import com.grierforensics.danesmimeatoolset.util.DstTestValues._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest._

import scala.collection.mutable.ListBuffer

class EmailTests extends FunSuite with BeforeAndAfterAll with LazyLogging {

  test("send and fetch email with signing and encryption") {
    val sender = EmailSender
    val fetcher = EmailFetcher
    //    val fetcher = new EmailFetcher(bobPopHost, bobAddress.getAddress, bobEmailPassword)

    val subject: String = "test email: " + System.currentTimeMillis()
    val text: String = "secret message"
    val email = Email(bobAddress, dstAddress, subject, text)

    sender.send(email)
    Thread.sleep(5000)
    sender.send(testDss.sign(email, bobIdentity))
    Thread.sleep(5000)
    sender.send(testDss.signAndEncrypt(email, bobIdentity, dstIdentity.getX509Certificate))
    Thread.sleep(5000)

    val fetched = ListBuffer[MessageDetails]()
    def handle(message: Message): Boolean = {
      if (message.getSubject != subject)
        return false

      fetched += testDss.inspectMessage(message, bobIdentity.getX509Certificate, testIdentity)
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
    val email = Email(bobAddress, dstAddress, subject, text)

    val fetched = ListBuffer[MessageDetails]()
    def handler(message: Message): Boolean = {
      if (message.getSubject != subject)
        return false

      fetched += testDss.inspectMessage(message, bobIdentity.getX509Certificate, dstIdentity)
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







