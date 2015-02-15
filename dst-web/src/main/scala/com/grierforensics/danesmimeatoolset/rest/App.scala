package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response
import javax.ws.rs.ext.{ContextResolver, ExceptionMapper, Provider}

import com.grierforensics.danesmimeatoolset.service.GensonConfig
import com.owlike.genson.Genson
import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import com.typesafe.scalalogging.LazyLogging
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.model.ContractProvider
import org.glassfish.jersey.server.ResourceConfig


class App extends ResourceConfig {
  register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
  register(new CatchAllExceptionMapper, ContractProvider.NO_PRIORITY)
  register(classOf[WorkflowResource])

}

object App {
  lazy val clientConfig = new ClientConfig().register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)

  def newClient = ClientBuilder.newClient(clientConfig)
}

@Provider
class CatchAllExceptionMapper extends ExceptionMapper[Exception] with LazyLogging {
  def toResponse(ex: Exception): Response = {
    ex match {
      case e: WebApplicationException => e.getResponse
      case e: Exception => {
        logger.warn("request failed", ex)
        Response.status(500).entity(ex.getMessage()).build()
      }
    }
  }
}

@Provider
class GensonCustomResolver extends ContextResolver[Genson] {
  override def getContext(`type`: Class[_]): Genson = GensonConfig.genson
}

