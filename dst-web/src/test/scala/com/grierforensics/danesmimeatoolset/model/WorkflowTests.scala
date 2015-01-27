package com.grierforensics.danesmimeatoolset.model

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
}
