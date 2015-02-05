package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation.Builder
import javax.ws.rs.core.Response.Status.Family
import javax.ws.rs.core.{Form, MediaType, Response}


trait RestClient {

  def get[T](url: String): String = get(url, classOf[String])


  def get[T](url: String, clazz: Class[T]): T = {
    val client: Builder = App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE)
    handleResponse(client.get(), clazz)
  }


  def post(url: String, params: Map[String, String]): String = post(url, params, classOf[String])


  def post[T](url: String, params: Map[String, String], clazz: Class[T]): T = {
    val form = new Form()
    for ((k, v) <- params)
      form.param(k, v)
    val entity: Entity[Form] = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE)

    val client: Builder = App.newClient.target(url).request(MediaType.APPLICATION_JSON_TYPE)
    handleResponse(client.post(entity), clazz)
  }


  def handleResponse[T](response: Response, clazz: Class[T]): T = {
    if (response.getStatusInfo.getFamily == Family.SUCCESSFUL) {
      response.readEntity(clazz)
    } else {
      throw new Exception("Can't handle response yet... " + response.getStatus + "\n" + response.readEntity(classOf[String]))
    }
  }
}
