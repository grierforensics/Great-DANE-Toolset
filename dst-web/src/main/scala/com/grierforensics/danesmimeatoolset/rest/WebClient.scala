// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs.client.{Client, ClientBuilder}

import com.owlike.genson.ext.jaxrs.GensonJsonConverter
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.model.ContractProvider

/** Factory object for clientConfig's suitable for making requests to WebService.
  *
  * Both WebClient and WebService have the same JSON serialization settings.
  *
  * Example usage:
  * {{{
  *   WebClient().target(url).request(MediaType.APPLICATION_JSON_TYPE).get().readEntity(classOf[SomeClass])
  * }}}
  * */
object WebClient {
  private lazy val clientConfig = new ClientConfig().register(new GensonJsonConverter(new GensonCustomResolver), ContractProvider.NO_PRIORITY)
  private val client: Client = ClientBuilder.newClient(clientConfig)

  def apply() = {
    client
  }
}
