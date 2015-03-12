package com.grierforensics.danesmimeatoolset.util

import scala.collection.mutable
import scala.util.Random


/** Generates an epoch-ish id with a random tail and a string prefix.
  * Epochishness degrades if an average of more than one id is generated per ms. */
class IdGenerator(val name: String = "") {

  def nextId: String = {
    synchronized {
      lastId = Math.max(lastId + 1, System.currentTimeMillis())
      "%s%d%d3".format(name, lastId, Random.nextInt(10000))
    }
  }

  private var lastId: Long = 0
}


/** Factory'ish singleton for IdGenerator's.
  *
  * A map of idGenerators is stored, one for each name prefix.  If a new name prefix is encountered, then a new
  * IdGenerator will be created. */
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
