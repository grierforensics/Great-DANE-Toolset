package com.grierforensics.danesmimeatoolset.model

import java.util.Date

import scala.beans.BeanProperty


abstract class Event {
  def eventType: EventType
  def message: String
  def date: Date
}


class BasicEvent(@BeanProperty val eventType: EventType,
                 @BeanProperty val message: String,
                 @BeanProperty val date: Date = new Date) extends Event {

  def canEqual(other: Any): Boolean = other.isInstanceOf[BasicEvent]

  override def equals(other: Any): Boolean = other match {
    case that: BasicEvent =>
      (that canEqual this) &&
        eventType == that.eventType &&
        message == that.message &&
        date == that.date
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(eventType, message, date)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
