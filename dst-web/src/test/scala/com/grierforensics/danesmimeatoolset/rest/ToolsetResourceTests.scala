package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.NotFoundException
import javax.ws.rs.core.GenericType

import com.grierforensics.danesmimeatoolset.util.DstTestValues._
import org.scalatest._;


class ToolsetResourceTests extends FunSuite with BeforeAndAfterAll with JsonRestClient {

  val server = TestServer.instance
  val listStringType = new GenericType[List[String]]() {}

  test("REST toolset lookup cert") {
    val certs = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/text", listStringType)
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithoutDane)}"))

    val cert = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/text/0", classOf[String])
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/10", classOf[String]))
  }


  test("REST toolset lookup cert hex") {
    val certHexs = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/hex", listStringType)
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithoutDane)}/hex", listStringType))

    val certHex = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/hex/0", classOf[String])
    assert(certHex == emailWithDaneCertHex)
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/hex/10", classOf[String]))
  }


  test("REST toolset lookup dnsZoneLine") {
    val dnsZoneLines = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLine", listStringType)
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithoutDane)}/dnsZoneLine", listStringType))

    val dnsZoneLine = get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLine/0", classOf[String])
    intercept[NotFoundException](get(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLine/10", classOf[String]))
  }


  test("REST toolset create dnsZoneLine") {
    val dnsZoneLine = post(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLineForCert", emailWithDaneCertHex, classOf[String])
    assert(dnsZoneLine == emailWithDanePublishedZoneLine)
  }
}


