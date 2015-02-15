package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation.Builder
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response.Status.Family
import javax.ws.rs.core.{Form, MediaType, Response}


/**
 * Trait that adds methods to conveniently access a Json based REST webservice.  All responses are assumed to be JSON
 * and can automatically be deserialized to a specified return class.  This is backed by javax.ws.rs.client generated
 * by App.newClient (this is somewhat hard coded for now, and should be replaced by some passed in ClientConfig)
 */
trait JsonRestClient {

  def get[T](url: String): String = get(url, classOf[String])


  def get[T](url: String, returnClass: Class[T]): T = {
    val client: Builder = App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE)
    handleResponse(client.get(), returnClass)
  }


  def post(url: String, body: Any): String = post(url, body, classOf[String])


  def post[T](url: String, any: Any, returnClass: Class[T]): T = {
    val entity: Entity[Any] = Entity.entity(any, MediaType.APPLICATION_JSON)
    post(url, entity, returnClass)
  }


  def post(url: String, body: Entity[_]): String = post(url, body, classOf[String])


  def post[T](url: String, entity: Entity[_], returnClass: Class[T]): T = {
    val client: Builder = App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE)
    handleResponse(client.post(entity), returnClass)
  }


  def postForm(url: String, params: Map[String, String]): String = postForm(url, params, classOf[String])


  def postForm[T](url: String, params: Map[String, String], returnClass: Class[T]): T = {
    val form = new Form()
    for ((k, v) <- params)
      form.param(k, v)
    val entity: Entity[Form] = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE)

    val client: Builder = App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE)
    handleResponse(client.post(entity), returnClass)
  }


  def handleResponse[T](response: Response, returnClass: Class[T]): T = {
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL)
      return response.readEntity(returnClass)

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
      case _ => new Exception("Trouble " + response.getStatus + "\n" + response.readEntity(classOf[String]))
    }
    throw e;
  }
}
