package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.InternalServerErrorException

import com.grierforensics.danesmimeatoolset.model.Workflow
import com.grierforensics.danesmimeatoolset.service.GensonConfig._
import org.scalatest._

/** Test workflow functionality.
  * todo: fill in many more tests...*/
class WorkflowResourceTests extends FunSuite with JsonRestClient {

  val server = TestServer.instance

  test("GET /") {
    val res = get(s"${server.url}")
    //res.lines.take(5) foreach println
    assert(res.startsWith("<!DOCTYPE html>"))
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


