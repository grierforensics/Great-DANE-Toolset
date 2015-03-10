package com.grierforensics.danesmimeatoolset.rest

import java.io.InputStream
import java.net.URLEncoder
import javax.ws.rs._
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response.Status.Family
import javax.ws.rs.core.{Form, GenericType, MediaType, Response}

import com.grierforensics.danesmimeatoolset.service.GensonConfig

import scala.io.{Codec, Source}


/**
 * Trait that adds methods to conveniently access a Json based REST webservice.  All responses are assumed to be JSON
 * and can automatically be deserialized to a specified return class.  This is backed by javax.ws.rs.client generated
 * by App.newClient (this is somewhat hard coded for now, and should be replaced by some passed in ClientConfig)
 */
trait JsonRestClient {

  //get
  def getResponse[T](url: String): Response = {
    App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE).get()
  }

  def get[T](url: String): String = handleResponse(getResponse(url), classOf[String])

  def get[T](url: String, entityType: Class[T]): T = handleResponse(getResponse(url), entityType)

  def get[T](url: String, entityType: GenericType[T]): T = handleResponse(getResponse(url), entityType)


  //post

  def postToResponse[T](url: String, any: Any): Response = {
    val entity: Entity[Any] = Entity.entity(any, MediaType.APPLICATION_JSON)
    App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(entity)
  }

  def post[T](url: String, any: Any): String = handleResponse(postToResponse(url, any), classOf[String])

  def post[T](url: String, any: Any, entityType: Class[T]): T = handleResponse(postToResponse(url, any), entityType)

  def post[T](url: String, any: Any, entityType: GenericType[T]): T = handleResponse(postToResponse(url, any), entityType)

  def postForMediaType(url: String, any: Any,
                       requestMediaType: String = MediaType.TEXT_PLAIN,
                       responseMediaType: String = MediaType.TEXT_PLAIN): String = {
    val entity: Entity[Any] = Entity.entity(any, requestMediaType)
    val response: Response = App.newClient.target(url).request(responseMediaType).post(entity)

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

    App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(entity)
  }

  def postForm[T](url: String, params: Map[String, String]): String =
    handleResponse(postFormToResponse(url, params), classOf[String])

  def postForm[T](url: String, params: Map[String, String], entityType: Class[T]): T =
    handleResponse(postFormToResponse(url, params), entityType)

  def postForm[T](url: String, params: Map[String, String], entityType: GenericType[T]): T =
    handleResponse(postFormToResponse(url, params), entityType)


  //response handling

  def handleResponse[T](response: Response, entityType: Class[T]): T = {
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL) {
      val result: T = response.readEntity(entityType)
      //workaround because Jersey skips deserializing string results (JSON is already a string)
      result match {
        case s: String if entityType == classOf[String] => GensonConfig.genson.deserialize(s, entityType)
        case otherwise => otherwise
      }
    } else
      throwForResponse(response)
  }


  def handleResponse[T](response: Response, entityType: GenericType[T]): T = {
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL) {
      val result: T = response.readEntity(entityType)
      //workaround because Jersey skips deserializing string results (JSON is already a string)
      result match {
        case s: String if entityType.getRawType == classOf[String] => GensonConfig.genson.deserialize(s, classOf[String]).asInstanceOf[T]
        case otherwise => otherwise
      }
    } else
      throwForResponse(response)
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
}
