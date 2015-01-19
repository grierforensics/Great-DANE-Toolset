package com.grierforensics.danesmimeatoolset

import java.util
import javax.ws.rs.core.Application
import javax.ws.rs.ext.{ContextResolver, Provider}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.common.collect.ImmutableSet

class App extends Application {
  override def getClasses: util.Set[Class[_]] = {
    ImmutableSet.of(
      classOf[RestService]
    )
  }

  override def getSingletons: util.Set[AnyRef] = {
    ImmutableSet.of(
      new ObjectMapperProvider
    )
  }
}

@Provider
class ObjectMapperProvider extends ContextResolver[ObjectMapper] {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  override def getContext(typ: Class[_]): ObjectMapper = mapper
}