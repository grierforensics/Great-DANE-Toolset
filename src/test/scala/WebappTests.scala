package com.grierforensics.danesmimeatoolset

import java.net._

import org.scalatest._

import scala.io.Source.fromInputStream

class WebappTests extends FunSuite with BeforeAndAfterAll {

  val testServer = new TestServer("src/main/webapp", 63636, "/")

  override def afterAll() {
    testServer.stop()
  }

  def get(url: String): String = {
    val conn = (new URL(url)).openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    fromInputStream(conn.getInputStream).getLines().mkString("\n")
  }

  test("GET /") {
    val res = get("http://localhost:8080/")
    //res.lines.take(5) foreach println
    assert(res.startsWith("<!DOCTYPE html>"))
  }

  test("GET /test/echo") {
    val res = get("http://localhost:8080/test/blah")
    //println(res)
    assert(res.contains("\"echo\":\"blah\""))
  }

}
