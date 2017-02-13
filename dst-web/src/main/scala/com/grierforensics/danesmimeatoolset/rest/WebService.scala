// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.{ContextResolver, ExceptionMapper, Provider}

import com.grierforensics.danesmimeatoolset.service.Context
import com.grierforensics.danesmimeatoolset.service.GensonConfig.genson
import com.owlike.genson.Genson
import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import com.typesafe.scalalogging.LazyLogging
import org.glassfish.jersey.model.ContractProvider
import org.glassfish.jersey.server.ResourceConfig


/** Web application configuration class.
  *
  * This is the entry point for setting up a Jersey base WebApplication.
  * WebService should be referenced in web.xml
  *
  * Example reference:
  * {{{
  * <filter>
  *  <filter-name>Jersey Filter</filter-name>
  *  <filter-class>org.glassfish.jersey.servlet.ServletContainer</filter-class>
  *  <init-param>
  *    <param-name>javax.ws.rs.Application</param-name>
  *    <param-value>com.grierforensics.danesmimeatoolset.rest.WebService</param-value>
  *  </init-param>
  *  ...
  * }}}
  * */
class WebService extends ResourceConfig with LazyLogging {

  // App initialization

  register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
  register(new CatchAllExceptionMapper, ContractProvider.NO_PRIORITY)
  register(classOf[WorkflowResource])
  register(classOf[ToolsetResource])

  val context = Context
  context.fetcherStart()
}


/** Provides exception handling for Jersey */
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


/** Provides Genson instance for JSON handling in Jersey */
@Provider
class GensonCustomResolver extends ContextResolver[Genson] {
  override def getContext(`type`: Class[_]): Genson = genson
}
