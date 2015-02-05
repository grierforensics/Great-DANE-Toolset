package com.grierforensics.danesmimeatoolset.rest

import java.nio.file.{Files, Paths}

import com.grierforensics.danesmimeatoolset.model.Workflow
import org.scalatest._;


class WebappTests extends FunSuite with BeforeAndAfterAll with RestClient {

  val projectRoot = if (Files.exists(Paths.get("dst-web"))) "dst-web/" else "./"

  val testServer = new TestServer(projectRoot + "src/main/webapp", 63636, "/")

  override def afterAll() {
    testServer.stop()
  }

  test("GET /") {
    val res = get("http://localhost:63636/")
    //res.lines.take(5) foreach println
    assert(res.startsWith("<!DOCTYPE html>"))
  }

  test("GET /workflow/echo/blah") {
    val r1 = get("http://localhost:63636/workflow/echo/blah1", classOf[Echo])
    val r2 = get("http://localhost:63636/workflow/echo/blah2", classOf[Echo])
    //println(res)
    assert(r1.echo == "blah1")
    assert(r2.echo == "blah2")
    assert(r2.count - r1.count == 1)
  }

  test("REST Workflow") {
    val wfStr1 = post("http://localhost:63636/workflow", Map("email" -> "dst.bob@example.com"), classOf[String])
    println(wfStr1)
    val wf1: Workflow = App.genson.deserialize(wfStr1, classOf[Workflow])

    val wfStr2 = get("http://localhost:63636/workflow/" + wf1.id, classOf[String])
    println(wfStr2)
    val wf2: Workflow = App.genson.deserialize(wfStr2, classOf[Workflow])

    assert(wf1 == wf2)
    println(wf1)
  }

}


