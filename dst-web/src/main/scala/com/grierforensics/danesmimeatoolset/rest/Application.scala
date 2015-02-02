package com.grierforensics.danesmimeatoolset.rest

import java.util
import javax.ws.rs.core.Application

import jersey.repackaged.com.google.common.collect.ImmutableSet

class App extends Application {
  override def getClasses: util.Set[Class[_]] = {
    ImmutableSet.of(
      classOf[WorkflowResource]
    )
  }

  override def getSingletons: util.Set[AnyRef] = {
    ImmutableSet.of(
//      new ObjectMapperProvider
    )
  }
}

//@Provider
//class ObjectMapperProvider extends ContextResolver[ObjectMapper] {
//  val mapper = new ObjectMapper() with ScalaObjectMapper
//  mapper.registerModule(DefaultScalaModule)
//
//  override def getContext(typ: Class[_]): ObjectMapper = mapper
//}