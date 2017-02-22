// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.rest

import java.io.InputStream
import java.lang.reflect.Type
import java.net.URLEncoder
import javax.ws.rs._
import javax.ws.rs.client.{Client, ClientBuilder, Entity}
import javax.ws.rs.core.Response.Status.Family
import javax.ws.rs.core.{Form, GenericType, MediaType, Response}

import com.grierforensics.danesmimeatoolset.service.GensonConfig
import com.owlike.genson
import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.model.ContractProvider

import scala.io.{Codec, Source}


/**
 * Trait that adds methods to conveniently access a Json based REST webservice.  All responses are assumed to be JSON
 * and can automatically be deserialized to a specified return class.  This is backed by javax.ws.rs.client generated
 * by App.newClient (this is somewhat hard coded for now, and should be replaced by some passed in ClientConfig)
 */
trait JsonRestClient {
  import JsonRestClient._

  //get
  def getResponse[T](url: String): Response = {
    client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get()
  }

  def get[T](url: String): String = handleResponse(getResponse(url))

  def get[T](url: String, entityType: Class[T]): T = handleResponse(getResponse(url), entityType)

  def get[T](url: String, entityType: GenericType[T]): T = handleResponse(getResponse(url), entityType)


  //post

  def postToResponse[T](url: String, any: Any): Response = {
    val entity: Entity[Any] = Entity.entity(any, MediaType.APPLICATION_JSON)
    client.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(entity)
  }

  def post[T](url: String, any: Any): String = handleResponse(postToResponse(url, any))

  def post[T](url: String, any: Any, entityType: Class[T]): T = handleResponse(postToResponse(url, any), entityType)

  def post[T](url: String, any: Any, entityType: GenericType[T]): T = handleResponse(postToResponse(url, any), entityType)

  def postForMediaType(url: String, any: Any,
                       requestMediaType: String = MediaType.TEXT_PLAIN,
                       responseMediaType: String = MediaType.TEXT_PLAIN): String = {
    val entity: Entity[Any] = Entity.entity(any, requestMediaType)
    val response: Response = client.target(url).request(responseMediaType).post(entity)

    if (response.getStatusInfo.getFamily != Family.SUCCESSFUL)
      throwForResponse(response)

    response.getEntity() match {
      case is: InputStream => Source.fromInputStream(is)(Codec.UTF8).mkString
      case otherwise => null
    }
  }

  //post form

  def postFormToResponse[T](url: String, params: Map[String, String]): Response = {
    val form = new Form()
    for ((k, v) <- params)
      form.param(k, v)
    val entity: Entity[Form] = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE)

    client.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(entity)
  }

  def postForm[T](url: String, params: Map[String, String]): String =
    handleResponse(postFormToResponse(url, params))

  def postForm[T](url: String, params: Map[String, String], entityType: Class[T]): T =
    handleResponse(postFormToResponse(url, params), entityType)

  def postForm[T](url: String, params: Map[String, String], entityType: GenericType[T]): T =
    handleResponse(postFormToResponse(url, params), entityType)


  //response handling

  def handleResponse(response: Response): String = {
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL) {
      response.readEntity(classOf[String])
    } else
      throwForResponse(response)
  }


  def handleResponse[T](response: Response, entityType: Class[T]): T = {
    val json: String = handleResponse(response)
    GensonConfig.genson.deserialize(json, entityType)
  }


  def handleResponse[T](response: Response, entityType: GenericType[T]): T = {
    val json: String = handleResponse(response)
    GensonConfig.genson.deserialize(json, new GensonGenericTypeWrapper(entityType))
  }


  def throwForResponse[T](response: Response): Nothing = {
    val e = response.getStatus match {
      case 400 => new BadRequestException(response)
      case 401 => new NotAuthorizedException(response)
      case 403 => new ForbiddenException(response)
      case 404 => new NotFoundException(response)
      case 405 => new NotAllowedException(response)
      case 406 => new NotAcceptableException(response)
      case 415 => new NotSupportedException(response)
      case 500 => new InternalServerErrorException(response)
      case 503 => new ServiceUnavailableException(response)
      case _ => new scala.Exception("Trouble " + response.getStatus + "\n" + response.readEntity(classOf[String]))
    }
    throw e;
  }


  // util

  def urlEncode(s: String): String = {
    URLEncoder.encode(s, "UTF-8")
  }


  /** Represents javax.ws.rs.core.GenericType as a Genson GenericType */
  class GensonGenericTypeWrapper[T](gt: GenericType[T]) extends genson.GenericType[T] {
    override def getType: Type = gt.getType

    override def getRawClass: Class[T] = gt.getRawType.asInstanceOf[Class[T]]
  }

}

/** Factory object for clientConfig's suitable for making requests to WebService.
  *
  * Both WebClient and WebService have the same JSON serialization settings.
  *
  * Example usage:
  * {{{
  *   client.target(url).request(MediaType.APPLICATION_JSON_TYPE).get().readEntity(classOf[SomeClass])
  * }}}
  * */
object JsonRestClient {
  private val client: Client = {
    val clientConfig = new ClientConfig().register(
      new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
    ClientBuilder.newClient(clientConfig)
  }
}
