package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}

import com.grierforensics.danesmimeatoolset.service.{BadCertificateException, Context, GensonConfig}
import org.bouncycastle.cert.dane.DANEEntry
import org.bouncycastle.util.encoders.{DecoderException, Hex}

/** WebService resource exposing DANE SMIME look up and generation functionality. */
@Path("/toolset")
@Produces(Array(MediaType.APPLICATION_JSON))
class ToolsetResource {
  val daneSmimeaService = Context.daneSmimeaService

  // // // text lookup

  @GET
  @Path("{email}/text")
  def lookupText(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchDaneCerts(email).map(_.toString) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  @GET
  @Path("{email}/text/{index}")
  def lookupText(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    val result: String = lookupText(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
    GensonConfig.genson.serialize(result)
  }

  // // // hex lookup

  @GET
  @Path("{email}/hex")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def lookupHex(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchDaneCerts(email).map(cert => Hex.toHexString(cert.getEncoded).toUpperCase) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  /** LookupHex by index */
  @GET
  @Path("{email}/hex/{index}")
  def lookupHex(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    val result = lookupHex(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
    GensonConfig.genson.serialize(result)
  }

  // // // lookupDnsZoneLine

  @GET
  @Path("{email}/dnsZoneLine")
  def lookupDnsZoneLine(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchDaneEntries(email).map(daneSmimeaService.getDnsZoneLineForDaneEntry(_)) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  @GET
  @Path("{email}/dnsZoneLine/{index}")
  def lookupDnsZoneLine(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    val result = lookupDnsZoneLine(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
    GensonConfig.genson.serialize(result)
  }

  // // // createDnsZoneLineAsText

  @POST
  @Path("{email}/dnsZoneLineForCert")
  @Consumes(Array(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN))
  def createDnsZoneLineAsText(@PathParam("email") email: String, certHex: String): String = {
    try {
      val de2: DANEEntry = daneSmimeaService.createDANEEntry(email, Hex.decode(certHex))
      val result = daneSmimeaService.getDnsZoneLineForDaneEntry(de2)
      GensonConfig.genson.serialize(result)
    }
    catch {
      case e@(_: BadCertificateException | _: DecoderException) => throw new WebApplicationException(Response.status(400).entity(e.getMessage).build())
    }
  }
}
