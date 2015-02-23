package com.grierforensics.danesmimeatoolset.model

import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.service.EmailSender
import com.grierforensics.danesmimeatoolset.service.GensonConfig.genson
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class JsonTests extends FunSuite with BeforeAndAfterAll {
  val message = EmailSender.createMessage(Email(new InternetAddress("a@b.c"), new InternetAddress("x@y.z"), "subject", "body"))

  test("json InternetAddress (problem class)") {
    val ia1: InternetAddress = new InternetAddress("a@b.c", "Bob")
    val j1: String = genson.serialize(ia1)
    println(j1)
    val ia2: InternetAddress = genson.deserialize(j1, classOf[InternetAddress])
    assert(ia1 == ia2)
  }

  test("json serialization") {
    val w1 = Workflow("dst.bob@example.com")
    w1.updateCert()
    w1.handleMessage(message)

    val j1: String = genson.serialize(w1)
    println(j1)
    //val w2: Workflow = genson.deserialize(j1,classOf[Workflow])
    //assert(w1===w2)
  }
}
