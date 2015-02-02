package com.grierforensics.danesmimeatoolset.model

import com.owlike.genson.{ScalaGenson, ScalaBundle, GensonBuilder}
import com.owlike.genson.ext.json4s.Json4SBundle
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class WorkflowTests extends FunSuite with BeforeAndAfterAll {

  test("happy path") {
    val w1 = Workflow("bob.dst@example.com")
    w1.sendEmail()
    assert(w1.events.size == 3)

    WorkflowDao.persist(w1)
    val w2 = WorkflowDao.fetch(w1.id)
    assert(w2 === w1)
  }

  test("json generation") {
    val w1 = Workflow("bob.dst@example.com")
    w1.updateCert()
    val genson = new GensonBuilder().create()
    val json1: String = genson.serialize(w1)
    println(json1)


  }
}
