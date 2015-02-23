package com.grierforensics.danesmimeatoolset.model

import java.security.cert.X509Certificate
import java.util
import java.util.Map.Entry
import javax.mail.Message
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.EventType._
import com.grierforensics.danesmimeatoolset.model.Workflow._
import com.grierforensics.danesmimeatoolset.service.{DaneSmimeService, EmailSender, MessageDetails}
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.encoders.Base64

import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer
import scala.util.Random

///////////////////////////// Workflow

class Workflow(@BeanProperty val id: String,
               @BeanProperty val emailAddress: String,
               var cert: Option[X509Certificate] = None,
               @BeanProperty var events: ListBuffer[Event] = ListBuffer()) extends LazyLogging {

  @BeanProperty val replyToAddress: String = dstAddress.getAddress
  @BeanProperty val replyCert: String = dstCertBase64Str

  val to: InternetAddress = new InternetAddress(emailAddress)
  val clickHost = config.getString("Workflow.clickHostUrl")

  events += new Event(waiting, "Starting validation ...")


  def sendEmail(): Unit = {
    val email = createEmail

    val signedEmail: Email = daneSmimeService.sign(email, dstIdentity)

    updateCert() match {
      case Some(c) => {
        val encryptedEmail: Email = daneSmimeService.encrypt(signedEmail, c)
        sender.send(encryptedEmail)
        events += new Event(success, "Sent encrypted email.")
        sender.send(signedEmail)
        events += new Event(success, "Sent signed email as backup.")
      }
      case None => {
        sender.send(signedEmail)
        events += new Event(success, "Sent signed email.")
      }
    }

    events += new Event(waiting, "Please check your email ...")
  }


  def receivedSignedBad(): Unit = {
    events += new Event(EventType.error, "Clicked: Bad Signature")
    dropWaiting()
  }


  def receivedSignedOk(): Unit = {
    events += new Event(EventType.success, "Clicked: Good Signature")
    dropWaiting()
  }


  def createEmail: Email = {
    Email(dstAddress, to,
      s"Test Mail from DANE SMIMEA Toolset ($id)",
      s"""Hello,

         This is a test email sent by the DANE SMIMEA Toolset.
         If you do not want to receive these emails ... TBD

         At this point, we can tell you:

         - $certDescription

         You may do any of the following options:

         - Reply to this email and a report will be emailed back.
            Then click here to followup and see the result:
            $clickHost/#/workflow/$id

         - If this email looks good and the signature appears valid click here:
            $clickHost/workflow/$id/click/receivedSignedOk?uiRedirect=true

         - If email signature appears broken click here:
            $clickHost/workflow/$id/click/receivedSignedBad?uiRedirect=true

         To learn more about DANE SMIME here are some helpful links: ... TBD
     """.stripMargin
    )
  }


  def updateCert(): Option[X509Certificate] = {
    val fetched: Option[X509Certificate] = daneSmimeService.fetchCert(emailAddress)
    fetched match {
      case Some(f) if cert.isEmpty => {
        events += new Event(validCert, "Found cert :" + f) //todo: create cert event with proper message
        cert = fetched
      }
      case Some(f) if cert.isDefined && f != cert.get => {
        events += new Event(validCert, "Found updated cert " + f) //todo: create cert event with proper message
        cert = fetched
      }
      case None if cert.isDefined => {
        events += new Event(invalidCert, "DANE Cert is missing.  Using previously loaded cert " + cert.get)
      }
      case None if cert.isEmpty => {
        events += new Event(invalidCert, "DANE Cert is not found.")
      }
      case _ => {}
    }
    dropWaiting()

    logger.info(s"Updated cert: id=$id found=${fetched.isDefined} ")
    cert
  }


  def handleMessage(message: Message) = {
    val md: MessageDetails = daneSmimeService.inspectMessage(message, dstIdentity, cert.orNull)
    events += new EmailReceivedEvent(emailReceived, "Email reply received!", md)
    dropWaiting()

    logger.info(s"Received email response: id=$id from=${md.from} ")
  }


  def dropWaiting() = {
    val newEvents: ListBuffer[Event] = events.filter(waiting != _.eventType)
    events = newEvents
  }

  def certDescription: String = {
    cert match {
      case Some(c) => s"You have a valid DANE SMIME record associated with your email address! ($emailAddress)\n"
      case None => s"Sorry, you do not have a valid DANE SMIME record associated with your email address ($emailAddress)"
    }
  }

  def getCertData: String = cert match {
    case Some(c) => c.toString
    case None => ""
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

  val dstAddress: InternetAddress = new InternetAddress(config.getString("Workflow.fromAddress"), config.getString("Workflow.fromName"))
  val dstIdentity = daneSmimeService.generateIdentity(dstAddress)
  val dstCertBase64Str = Base64.toBase64String(new X509CertificateHolder(dstIdentity.getX509Certificate.getEncoded).getEncoded) //todo: fix this needs testing

  def apply(email: String) = {
    new Workflow(nextId, email)
  }

  def parseIdInSubject(subject: String): Option[String] = {
    val idInSubjectPattern = """\((\d*)\)""".r
    idInSubjectPattern.findFirstMatchIn(subject) match {
      case Some(m) => Option(m.group(1))
      case None => None
    }
  }

  /**
   * Gets a epoch-ish id with a random tail
   */
  def nextId: String = {
    synchronized {
      lastId = Math.max(lastId + 1, System.currentTimeMillis())
      "%d%d3".format(lastId, Random.nextInt(1000))
    }
  }

  private var lastId: Long = 0

}

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




