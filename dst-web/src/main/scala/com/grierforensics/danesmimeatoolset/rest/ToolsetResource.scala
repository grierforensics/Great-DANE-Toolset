package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}

import com.grierforensics.danesmimeatoolset.service.{BadCertificateException, DaneSmimeaService, GensonConfig}
import org.bouncycastle.cert.dane.DANEEntry
import org.bouncycastle.util.encoders.{DecoderException, Hex}

@Path("/toolset")
@Produces(Array(MediaType.APPLICATION_JSON))
class ToolsetResource {
  val daneSmimeaService = DaneSmimeaService

  @GET
  @Path("{email}/text")
  def lookupText(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchCerts(email).map(_.toString) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  @GET
  @Path("{email}/text/{index}")
  def lookupText(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    lookupText(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
  }

  @GET
  @Path("{email}/hex")
  def lookupHex(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchCerts(email).map(cert => Hex.toHexString(cert.getEncoded).toUpperCase) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  @GET
  @Path("{email}/hex/{index}")
  def lookupHex(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    lookupHex(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
  }

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
    lookupDnsZoneLine(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).entity("DANE not found for email address").build())
    }
  }

  @POST
  @Path("{email}/dnsZoneLineForCert")
  @Consumes(Array(MediaType.TEXT_PLAIN))
  @Produces(Array(MediaType.TEXT_PLAIN))
  def createDnsZoneLineAsText(@PathParam("email") email: String, certHex: String): String = {
    try {
      val de2: DANEEntry = daneSmimeaService.createDANEEntry(email, Hex.decode(certHex))
      daneSmimeaService.getDnsZoneLineForDaneEntry(de2)
    }
    catch {
      case e@(_: BadCertificateException | _: DecoderException) => throw new WebApplicationException(Response.status(400).entity(e.getMessage).build())
    }
  }

  @POST
  @Path("{email}/dnsZoneLineForCert")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def createDnsZoneLineAsJson(@PathParam("email") email: String, certHex: String): String = {
    GensonConfig.genson.serialize(createDnsZoneLineAsText(email, certHex))
    //note: serializing here, because strings are assumed to be already serialized by Jersey and therefore aren't
    //      converted to JSON automatically.
  }
}

