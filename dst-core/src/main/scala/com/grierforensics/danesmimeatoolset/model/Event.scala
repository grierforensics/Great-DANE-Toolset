package com.grierforensics.danesmimeatoolset.model

import java.util.Date

import com.grierforensics.danesmimeatoolset.service.MessageDetails

import scala.beans.BeanProperty


class Event(@BeanProperty val eventType: EventType,
            @BeanProperty val message: String,
            @BeanProperty val date: Date = new Date) {

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