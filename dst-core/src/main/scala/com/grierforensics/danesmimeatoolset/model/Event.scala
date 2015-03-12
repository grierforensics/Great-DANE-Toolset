package com.grierforensics.danesmimeatoolset.model

import java.util.Date

import com.grierforensics.danesmimeatoolset.service.MessageDetails
import com.grierforensics.danesmimeatoolset.util.IdGenerator

import scala.beans.BeanProperty


/** Represents a generic Workflow event.
  *
  * Properties are marked as BeanProperties to enable JSON serialization. */
class Event(@BeanProperty val eventType: EventType,
            @BeanProperty val message: String,
            @BeanProperty val date: Date = new Date) {

  @BeanProperty val id: String = IdGenerator.nextId

  def canEqual(other: Any): Boolean = other.isInstanceOf[Event]

  override def equals(other: Any): Boolean = other match {
    case that: Event =>
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


/** Represents an event for when an email is received with MessageDetails
  *
  * Properties are marked as BeanProperties to enable JSON serialization. */
class EmailReceivedEvent(eventType: EventType,
                         message: String,
                         @BeanProperty val details: MessageDetails,
                         date: Date = new Date) extends Event(eventType, message, date) {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[EmailReceivedEvent]

  override def equals(other: Any): Boolean = other match {
    case that: EmailReceivedEvent =>
      (that canEqual this) &&
        eventType == that.eventType &&
        message == that.message &&
        details == that.details &&
        date == that.date
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(eventType, message, details, date)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}