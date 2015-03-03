package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.InternalServerErrorException

import com.grierforensics.danesmimeatoolset.model.Workflow
import com.grierforensics.danesmimeatoolset.service.GensonConfig._
import org.scalatest._


class WorkflowResourceTests extends FunSuite with JsonRestClient {

  val server = TestServer.instance

  test("GET /") {
    val res = get(s"${server.url}")
    //res.lines.take(5) foreach println
    assert(res.startsWith("<!DOCTYPE html>"))
  }

  test("GET /workflow/echo/blah") {
    val r1 = get(s"${server.url}/workflow/echo/blah1", classOf[Echo])
    val r2 = get(s"${server.url}/workflow/echo/blah2", classOf[Echo])
    //println(res)
    assert(r1.echo == "blah1")
    assert(r2.echo == "blah2")
    assert(r2.count - r1.count == 1)
  }

  test("REST Workflow") {
    val wfStr1 = post(s"${server.url}/workflow", "dst.bob@example.com")
    println(wfStr1)
    val wf1: Workflow = genson.deserialize(wfStr1, classOf[Workflow])

    val wfStr2 = get(s"${server.url}/workflow/" + wf1.id)
    //println(wfStr2)
    val wf2: Workflow = genson.deserialize(wfStr2, classOf[Workflow])

    assert(wfStr1 == wfStr2)
  }

  test("REST Bad Emails") {
    intercept[InternalServerErrorException] {
      post(s"${server.url}/workflow", "")
    }
    intercept[InternalServerErrorException] {
      post(s"${server.url}/workflow", "dst.bobexample.com")
    }

    //this  bad email should send and bounce silently
    post(s"${server.url}/workflow", "iauehpiu3hfpiuhiusadhiluhasfliuhlaisudhliuashf@example.com")
  }

}


