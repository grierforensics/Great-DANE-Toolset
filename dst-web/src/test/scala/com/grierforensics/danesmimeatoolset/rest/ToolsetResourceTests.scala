package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.core.GenericType
import javax.ws.rs.{BadRequestException, NotFoundException}

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
    val dnsZoneLine1 = postForMediaType(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLineForCert", emailWithDaneCertHex)
    assert(dnsZoneLine1 == emailWithDanePublishedZoneLine)

    val dnsZoneLine2 = post(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLineForCert", emailWithDaneCertHex, classOf[String])
    assert(dnsZoneLine2 == emailWithDanePublishedZoneLine)
  }

  test("REST toolset create dnsZoneLine with bad cert") {
    intercept[BadRequestException](
      post(s"${server.url}/toolset/${urlEncode(emailWithDane)}/dnsZoneLineForCert", badCertHex, classOf[String])
    )
  }
}


