package com.grierforensics.danesmimeatoolset.model

import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.EventType._
import com.grierforensics.danesmimeatoolset.model.Workflow._
import com.grierforensics.danesmimeatoolset.service.{EmailFetcher, DaneSmimeService, EmailSender}
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._

import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer
import scala.util.Random

///////////////////////////// Workflow

class Workflow(@BeanProperty val id: String,
               @BeanProperty val emailAddress: String,
               var cert: Option[X509Certificate] = None,
               @BeanProperty var events: ListBuffer[BasicEvent] = ListBuffer()) {
  //todo: hack BasicEvent used here until unmarshalling abstract class works


  def sendEmail(): Unit = {
    val email = createEmail

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


  def receivedSignedBad(): Unit = events += new BasicEvent(EventType.error, "Received email with bad signature.")


  def receivedSignedOk(): Unit = events += new BasicEvent(EventType.success, "Received email with good signature ")


  def createEmail: Email = {
    val clickHost = config.getString("Workflow.clickHostUrl")

    Email(EmailFetcher.address, new InternetAddress(emailAddress),
      "Test Mail from DANE SMIMEA Toolset (" + id + ")",
      s""" Hello,

         This is a test email sent by the DANE SMIMEA Toolset.
         If you do not want to receive these emails ... TBD

         At this point, we can tell you:

         - You do/doNot have a valid DANE SMIME record associated with your email address ... TBD

         You may do any of the following options:

         - Reply to this email and a report will be emailed back.

         - If this email looks good and the signature appears valid click here:
             ${clickHost}/workflow/${id}/click/receivedSignedOk?uiRedirect=true

         - If email signature appears broken click here:
             ${clickHost}/workflow/${id}/click/receivedSignedBad?uiRedirect=true

         To learn more about DANE SMIME here are some helpful links: ... TBD


      """)
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


  def canEqual(other: Any): Boolean = other.isInstanceOf[Workflow]

  override def equals(other: Any): Boolean = other match {
    case that: Workflow =>
      (that canEqual this) &&
        id == that.id &&
        emailAddress == that.emailAddress &&
        events == that.events
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(id, emailAddress, events)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}


object Workflow {

  val daneSmimeService = DaneSmimeService

  val sender = EmailSender

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
      "%d%d3".format(lastId, Random.nextInt(1000))
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




