package com.grierforensics.danesmimeatoolset.model

import com.grierforensics.danesmimeatoolset.rest.App
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

  test("json serialization") {
    val w1 = Workflow("dst.bob@example.com")
    w1.updateCert()

    val j1: String = App.genson.serialize(w1)
    val w2: Workflow = App.genson.deserialize(j1,classOf[Workflow])
    assert(w1===w2)
    println(j1)
  }
}
