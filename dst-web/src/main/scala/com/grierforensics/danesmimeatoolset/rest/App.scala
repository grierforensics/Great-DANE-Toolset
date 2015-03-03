package com.grierforensics.danesmimeatoolset.rest

import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response
import javax.ws.rs.ext.{ContextResolver, ExceptionMapper, Provider}

import com.grierforensics.danesmimeatoolset.model._
import com.grierforensics.danesmimeatoolset.service.EmailFetcher
import com.grierforensics.danesmimeatoolset.service.GensonConfig.genson
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import com.owlike.genson.stream.{ObjectReader, ObjectWriter}
import com.owlike.genson.{Context, Converter, Genson}
import com.typesafe.scalalogging.LazyLogging
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.model.ContractProvider
import org.glassfish.jersey.server.ResourceConfig


class App extends ResourceConfig with LazyLogging {
  register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
  register(new CatchAllExceptionMapper, ContractProvider.NO_PRIORITY)
  register(classOf[WorkflowResource])
  register(classOf[ToolsetResource])

  val fetcher = EmailFetcher
  fetcher.asyncFetchAndDelete(handler, config.getLong("EmailFetcher.period"))

  private def handler(message: Message): Boolean = {
    val id: Option[String] = Workflow.parseIdInSubject(message.getSubject)
    if (id.isEmpty)
      return false //ignore non workflow emails

    WorkflowDao.fetch(id.get) match {
      case Some(w) => w.handleMessage(message)
      case None => logger.info("Dumping email for unknown/expired workflow id:" + id.get)
    }

    true
  }
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
  override def getContext(`type`: Class[_]): Genson = genson
}
