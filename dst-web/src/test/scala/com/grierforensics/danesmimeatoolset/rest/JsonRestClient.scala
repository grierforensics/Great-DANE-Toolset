package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation.Builder
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


  def post[T](url: String, any:Any, returnClass: Class[T]): T = {
    val entity: Entity[Any] = Entity.entity(any, MediaType.APPLICATION_JSON)
    post(url,entity,returnClass)
  }


  def post(url: String, body:Entity[_]): String = post(url, body, classOf[String])


  def post[T](url: String, entity:Entity[_], returnClass: Class[T]): T = {
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
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL) {
      response.readEntity(returnClass)
    } else {
      throw new Exception("Can't handle response yet... " + response.getStatus + "\n" + response.readEntity(classOf[String]))
    }
  }
}
