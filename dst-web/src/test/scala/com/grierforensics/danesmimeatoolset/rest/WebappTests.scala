package com.grierforensics.danesmimeatoolset.rest

import java.net._
import java.nio.file.{Files, Paths}

import org.scalatest._

import scala.io.Source.fromInputStream

class WebappTests extends FunSuite with BeforeAndAfterAll {

  val projectRoot = if (Files.exists(Paths.get("dst-web"))) "dst-web/" else "./"

  val testServer = new TestServer(projectRoot + "src/main/webapp", 63636, "/")

  override def afterAll() {
    testServer.stop()
  }

  def get(url: String): String = {
    val conn = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    fromInputStream(conn.getInputStream).getLines().mkString("\n")
  }

  test("GET /") {
    val res = get("http://localhost:63636/")
    //res.lines.take(5) foreach println
    assert(res.startsWith("<!DOCTYPE html>"))
  }

  test("GET /workflow/echo/blah") {
    val res = get("http://localhost:63636/workflow/echo/blah")
    //println(res)
    assert(res.contains("\"echo\":\"blah\""))
  }

}
