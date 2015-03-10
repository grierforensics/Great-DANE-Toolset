package com.grierforensics.danesmimeatoolset.util

import scala.collection.mutable
import scala.util.Random


class IdGenerator(val name: String = "") {
  /**
   * Gets a epoch-ish id with a random tail.
   * Epochishness degrades if an average of more than one id is generated per ms.
   */
  def nextId: String = {
    synchronized {
      lastId = Math.max(lastId + 1, System.currentTimeMillis())
      "%s%d%d3".format(name, lastId, Random.nextInt(10000))
    }
  }

  private var lastId: Long = 0
}


object IdGenerator {
  val idGenerators = new mutable.HashMap[String, IdGenerator]

  def nextId :String = nextId("")

  def nextId(name: String): String = idGenerators.get(name) match {
    case Some(idGenerator) => idGenerator.nextId
    case None => {
      val idGenerator = new IdGenerator(name)
      idGenerators.put(name, idGenerator)
      idGenerator.nextId
    }
  }
}
