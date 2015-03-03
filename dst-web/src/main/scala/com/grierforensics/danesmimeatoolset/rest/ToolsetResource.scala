package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.core.{MediaType, Response}

import com.grierforensics.danesmimeatoolset.service.DaneSmimeaService
import org.bouncycastle.cert.dane.DANEEntry
import org.bouncycastle.util.encoders.Hex

@Path("/toolset")
@Produces(Array(MediaType.APPLICATION_JSON))
class ToolsetResource {
  val daneSmimeaService = DaneSmimeaService

  @GET
  @Path("{email}")
  def lookup(@PathParam("email") email: String): Seq[String] = {
    daneSmimeaService.fetchCerts(email).map(_.toString) match {
      case Nil => throw new WebApplicationException(Response.status(404).build())
      case nonempty => nonempty
    }
  }

  @GET
  @Path("{email}/{index}")
  def lookup(@PathParam("email") email: String, @PathParam("index") index: Int): String = {
    lookup(email) match {
      case results if results.isDefinedAt(index) => results(index)
      case otherwise => throw new WebApplicationException(Response.status(404).build())
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
      case otherwise => throw new WebApplicationException(Response.status(404).build())
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
      case otherwise => throw new WebApplicationException(Response.status(404).build())
    }
  }

  @POST
  @Path("{email}/dnsZoneLineForCert")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def createDnsZoneLine(@PathParam("email") email: String, certHex: String): String = {
    val de2: DANEEntry = daneSmimeaService.createDANEEntry(email, Hex.decode(certHex))
    daneSmimeaService.getDnsZoneLineForDaneEntry(de2)
  }
}

