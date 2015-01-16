package com.grierforensics.danesmimeatoolset

import javax.ws.rs._
import javax.ws.rs.core.MediaType


@Path("/test")
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class RestService {
  var count = 0;

  @GET
  @Path("{echo}")
  def getEcho(@PathParam("echo") echo: String): Echo = {
    count = count + 1
    new Echo(echo, count)
  }
}

class Echo(echo: String, count: Int) {
  def getEcho = echo //hack: Jackson isn't introspecting Scala properly.
  def getCount = count //hack: Jackson isn't introspecting Scala properly.
}