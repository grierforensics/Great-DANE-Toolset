// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.model

import java.util.Date

import com.grierforensics.danesmimeatoolset.service.MessageDetails
import com.grierforensics.danesmimeatoolset.util.IdGenerator

import scala.beans.BeanProperty

object EventType {
  type EventType = Int

  val sendingAuth = 0
  val error = 1
  val success = 2
  val validCert = 3
  val invalidCert = 4
  val emailReceived = 5
  val waiting = 6
  val message = 7
}

import EventType.EventType

trait Event {
  val eventType: EventType
  val message: String
}

/** Represents a generic Workflow event.
  *
  * Properties are marked as BeanProperties to enable JSON serialization. */
case class BasicEvent(eventType: EventType, message: String,
                      date: Date = new Date, id: String = IdGenerator.nextId) extends Event


/** Represents an event for when an email is received with MessageDetails
  *
  * Properties are marked as BeanProperties to enable JSON serialization. */
case class EmailReceivedEvent(eventType: EventType, message: String,
                              details: MessageDetails) extends Event
