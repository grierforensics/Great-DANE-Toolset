package com.grierforensics.danesmimeatoolset.model

import java.security.cert.X509Certificate
import java.util
import java.util.Map.Entry
import javax.mail.Message
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.EventType._
import com.grierforensics.danesmimeatoolset.model.Workflow._
import com.grierforensics.danesmimeatoolset.service.{DaneSmimeaService, EmailSendFailedException, EmailSender, MessageDetails}
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.encoders.Base64

import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import ExecutionContext.Implicits.global

///////////////////////////// Workflow

class Workflow(@BeanProperty val id: String,
               @BeanProperty val emailAddress: String,
               var cert: Option[X509Certificate] = None,
               @BeanProperty var events: ListBuffer[Event] = ListBuffer()) extends LazyLogging {

  @BeanProperty val replyToAddress: String = dstAddress.getAddress
  @BeanProperty val replyCert: String = dstCertBase64Str

  val to: InternetAddress = new InternetAddress(emailAddress)
  val clickHost = config.getString("Workflow.clickHostUrl")

  def sendEmail(): Unit = {
    val email = createEmail

    val signedEmail: Email = daneSmimeaService.sign(email, dstIdentity)

    updateCert() match {
      case Some(c) => {
        setWaiting("Sending email ...")
        val encryptedEmail: Email = daneSmimeaService.encrypt(signedEmail, c)
        sender.send(encryptedEmail)
        events += new Event(success, "Sent encrypted email.")
        sender.send(signedEmail)
        events += new Event(success, "Sent signed email as backup.")
      }
      case None => {
        setWaiting("Sending email ...")
        sender.send(signedEmail)
        events += new Event(success, "Sent signed email.")
      }
    }

    setWaiting("Please check your email ...")
  }


  def sendEmailAsync(): Unit = {
    Future {
      try {
        sendEmail()
      }
      catch {
        case e: EmailSendFailedException => {
          events += new Event(error, "Trouble sending email.")
          logger.error("Unable to send email", e)
        }
      }
    }
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
    setWaiting("Looking up DANE DNS ...")

    val fetched: Option[X509Certificate] = daneSmimeaService.fetchCert(emailAddress)
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
    val md: MessageDetails = daneSmimeaService.inspectMessage(message, dstIdentity, cert.orNull)
    events += new EmailReceivedEvent(emailReceived, "Email reply received!", md)
    dropWaiting()

    logger.info(s"Received email response: id=$id from=${md.from} ")
  }


  def setWaiting(message: String, dropOldWaiting: Boolean = true): Unit = {
    if (dropOldWaiting)
      dropWaiting()

    events += new Event(waiting, message)
  }


  def dropWaiting() = {
    events = events.filter(waiting != _.eventType)
  }

  def certDescription: String = {
    cert match {
      case Some(c) => s"You have a valid DANE SMIMEA record associated with your email address! ($emailAddress)\n"
      case None => s"Sorry, you do not have a valid DANE SMIMEA record associated with your email address ($emailAddress)"
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
  val daneSmimeaService = DaneSmimeaService
  val sender = EmailSender

  val dstAddress: InternetAddress = new InternetAddress(config.getString("Workflow.fromAddress"), config.getString("Workflow.fromName"))
  val dstIdentity = daneSmimeaService.generateIdentity(dstAddress)
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






