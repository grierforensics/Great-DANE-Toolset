package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response
import javax.ws.rs.ext.{ContextResolver, ExceptionMapper, Provider}

import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import com.owlike.genson.{Genson, GensonBuilder, ScalaBundle}
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
  val genson = new GensonBuilder().
    useIndentation(true).
    useRuntimeType(true).
    useDateAsTimestamp(true).
    withBundle(ScalaBundle().useOnlyConstructorFields(false)).
    create()

  lazy val clientConfig = new ClientConfig().
    register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)

  def newClient = ClientBuilder.newClient(clientConfig)
}

class Client extends ClientConfig {
  register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
}

@Provider
class CatchAllExceptionMapper extends ExceptionMapper[Exception] with LazyLogging {
  def toResponse(ex: Exception): Response = {
    logger.warn("request failed", ex)
    Response.status(500).entity(ex.getMessage()).build()
  }
}

@Provider
class GensonCustomResolver extends ContextResolver[Genson] {
  override def getContext(`type`: Class[_]): Genson = App.genson
}

