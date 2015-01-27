package com.grierforensics.danesmimeatoolset.model

import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

import com.grierforensics.danesmimeatoolset.model.EventType._
import com.grierforensics.danesmimeatoolset.model.Workflow._
import com.grierforensics.danesmimeatoolset.{DaneSmimeService, Email, EmailSender}

import scala.collection.mutable.ListBuffer
import scala.util.Random

///////////////////////////// Workflow

class Workflow private(val id: String,
                       val emailAddress: String,
                       var cert: Option[X509Certificate] = None,
                       var events: ListBuffer[Event] = ListBuffer()) {

  def sendEmail() = {
    val email = Email(Some(dstFromName), dstFromAddress, None, emailAddress,
      "Dane Smime Test (" + id + ")",
      "secret message with links for workflow id " + id)

    val signedEmail: Email = daneSmimeService.sign(email) //sign with generated identity

    updateCert() match {
      case Some(c) => {
        val encryptedEmail: Email = daneSmimeService.encrypt(signedEmail, c)
        sender.send(encryptedEmail)
        events += new BasicEvent(emailSent, "Sent encrypted email.")
      }
      case None => {
        sender.send(signedEmail)
        events += new BasicEvent(emailSent, "Sent signed email.")
      }
    }

    events += new BasicEvent(EventType.waiting, "Waiting for response...")
  }

  def updateCert(): Option[X509Certificate] = {
    val fetched: Option[X509Certificate] = daneSmimeService.fetchCert(emailAddress)
    fetched match {
      case Some(f) if cert.isEmpty => {
        events += new BasicEvent(validCert, "Found cert :" + f) //todo: create cert event with proper message
        cert = fetched
      }
      case Some(f) if cert.isDefined && f != cert.get => {
        events += new BasicEvent(validCert, "Found updated cert " + f) //todo: create cert event with proper message
        cert = fetched
      }
      case None if cert.isDefined => {
        events += new BasicEvent(message, "DANE Cert is missing.  Using previously loaded cert " + cert.get)
      }
      case None if cert.isEmpty => {
        events += new BasicEvent(message, "DANE Cert is not found.")
      }
      case _ => {}
    }
    cert
  }
}

object Workflow {

  val daneSmimeService = DaneSmimeService

  val sender = EmailSender

  val dstFromAddress: String = "test1.dst@example.com"

  val dstFromName: String = "Alice"


  def apply(email: String) = {
    new Workflow(nextId, email)
  }

  private var lastId: Long = 0

  /**
   * Gets a epoch-ish id with a random tail
   */
  def nextId: String = {
    synchronized {
      lastId = Math.max(lastId + 1, System.currentTimeMillis())
      lastId + "" + (Random.nextInt(9000000) + 1000000)
    }
  }
}


object WorkflowDao {
  val memoryWorkFlowCache = new ConcurrentHashMap[String, Workflow]()

  def persist(workflow: Workflow): Unit = {
    memoryWorkFlowCache.put(workflow.id, workflow)
  }

  def fetch(id: String) = {
    memoryWorkFlowCache.get(id)
  }
}

///////////////////////////// Events

abstract class Event {
  def eventType: EventType

  def message: String

  def date: Date
}

class BasicEvent(val eventType: EventType,
                 val message: String,
                 val date: Date = new Date) extends Event


