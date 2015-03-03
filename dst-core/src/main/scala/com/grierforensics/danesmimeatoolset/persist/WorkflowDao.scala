package com.grierforensics.danesmimeatoolset.persist

import java.util
import java.util.Map.Entry

import com.grierforensics.danesmimeatoolset.model.Workflow

/**
 * Hacked in memory persistence for now.
 */
object WorkflowDao {
  val cacheSize = 2000
  val memoryWorkFlowCache = new util.LinkedHashMap[String, Workflow](cacheSize + 1, .75F, true) {
    override def removeEldestEntry(eldest: Entry[String, Workflow]): Boolean = {
      return size() >= cacheSize; //size exceeded the max allowed
    }
  }

  def persist(workflow: Workflow): Unit = {
    memoryWorkFlowCache.synchronized(memoryWorkFlowCache.put(workflow.id, workflow))
  }

  def fetch(id: String): Option[Workflow] = {
    memoryWorkFlowCache.synchronized(Option(memoryWorkFlowCache.get(id)))
  }
}
