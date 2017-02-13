// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.model

import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.persist.WorkflowDao
import com.grierforensics.danesmimeatoolset.service.EmailSender
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/** Test basic workflow functionality.  Most tests should be implemented via the WorkflowResourceTest. */
class WorkflowTests extends FunSuite with BeforeAndAfterAll {

  val message = EmailSender.createMessage(Email(new InternetAddress("a@b.c"), new InternetAddress("x@y.z"), "subject", "body"))

  test("happy path") {
    val w1 = Workflow("dst.bob@example.com")
    w1.sendEmail()
    assert(w1.events.size == 3)

    WorkflowDao.persist(w1)
    val w2 = WorkflowDao.fetch(w1.id).get
    assert(w2 === w1)
  }

}
