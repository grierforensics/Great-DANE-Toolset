// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.model

import java.security.cert.X509Certificate
import javax.mail.Message
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.EventType._
import com.grierforensics.danesmimeatoolset.service._
import com.grierforensics.danesmimeatoolset.util.IdGenerator
import com.owlike.genson.annotation.{JsonIgnore, JsonProperty}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * Represents a DST Email testing workflow.
 *
 * State is tracked with a mutable list of events.
 */
case class Workflow(id: String, emailAddress: String) extends LazyLogging {
  def this(emailAddress: String) = this(IdGenerator.nextId, emailAddress)

  @JsonIgnore var cert: Option[X509Certificate] = None
  @JsonProperty var certData: String = ""

  @JsonProperty var events: ListBuffer[Event] = ListBuffer()

  @JsonIgnore val context = Context
  @JsonProperty val replyToAddress: String = context.dstAddress.getAddress
  @JsonProperty val replyCert: String = context.dstCertBase64Str


  /**
   * Sends test email to workflow emailAddress and adds events.
   * @param updateCertificate if true, the workflow cert will first be updated.
   */
  def sendEmail(updateCertificate: Boolean = true): Unit = {
    if (updateCertificate)
      updateCert()

    val email = createEmail
    val signedEmail: Email = context.daneSmimeaService.sign(email, context.dstIdentity)

    cert match {
      case Some(c) => {
        setWaiting("Sending email ...")
        val encryptedEmail: Email = context.daneSmimeaService.encrypt(signedEmail, c)
        context.sender.send(encryptedEmail)
        events += new BasicEvent(success, "Sent encrypted email.")
        context.sender.send(signedEmail)
        events += new BasicEvent(success, "Sent signed email as backup.")
      }
      case None => {
        setWaiting("Sending email ...")
        context.sender.send(signedEmail)
        events += new BasicEvent(success, "Sent signed email.")
      }
    }

    setWaiting("Please check your email ...")
  }


  /**
   * Sends test email to workflow emailAddress and adds events without blocking.
   * @param updateCertificate if true, the workflow cert will first be updated.
   */
  def sendEmailAsync(updateCertificate: Boolean = true): Unit = {
    Future {
      try {
        sendEmail(updateCertificate)
      }
      catch {
        case e: EmailSendFailedException => {
          events += new BasicEvent(error, "Trouble sending email.")
          logger.error("Unable to send email", e)
        }
      }
    }
  }


  /**
   * Records that the user indicates a bad signature;
   */
  def clickedReceivedSignedBad(): Unit = {
    events += new BasicEvent(EventType.error, "Clicked: Bad Signature")
    dropWaiting()
  }


  /**
   * Records that the user indicates a good signature;
   */
  def clickedReceivedSignedOk(): Unit = {
    events += new BasicEvent(EventType.success, "Clicked: Good Signature")
    dropWaiting()
  }


  /**
   * Checks for new DANE SMIMEA certificate, updates workflow if there is a new certificate, and adds an BasicEvent marking
   * to display what was found.
   */
  def updateCert(): Option[X509Certificate] = {
    setWaiting("Looking up DANE DNS ...")

    val fetched: Option[X509Certificate] = context.daneSmimeaService.fetchDaneCert(emailAddress)
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
        events += new BasicEvent(invalidCert, "DANE Cert is missing.  Using previously loaded cert " + cert.get)
      }
      case None if cert.isEmpty => {
        events += new BasicEvent(invalidCert, "DANE Cert is not found.")
      }
      case _ => {}
    }
    dropWaiting()

    // Update the String representation for JSON serialization
    certData = cert match {
      case Some(c) => c.toString
      case None => ""
    }

    logger.info(s"Updated cert: id=$id found=${fetched.isDefined} ")
    cert
  }


  /**
   * Inspects an email message for workflow and creates an event.
   */
  def handleMessage(message: Message) = {
    val md: MessageDetails = context.daneSmimeaService.inspectMessage(message, cert.orNull, context.dstIdentity)
    events += new EmailReceivedEvent(emailReceived, "Email reply received!", md)
    dropWaiting()

    logger.info(s"Received email response: id=$id from=${md.from} ")
  }


  /**
   * Adds a waiting message to the workflow.
   * @param message String to display
   * @param dropOldWaiting if true, all other waiting events will be cleared.
   */
  def setWaiting(message: String, dropOldWaiting: Boolean = true): Unit = {
    if (dropOldWaiting)
      dropWaiting()

    events += new BasicEvent(waiting, message)
  }


  /**
   * Removes all waiting events.
   */
  def dropWaiting() = {
    events = events.filter(waiting != _.eventType)
  }


  private def createEmail: Email = {
    val certDataAddendum = cert match {
      case Some(s) => "Certificate data:\n" + certData
      case None => ""
    }

    val certDescriptionForEmail = cert match {
      case Some(c) => {
        s"""You have a DANE SMIMEA record associated with your email address! ($emailAddress) Your certificate information is included at the bottom of this email."""
      }
      case None => s"Sorry, you do NOT have a DANE SMIMEA record associated with your email address ($emailAddress)"
    }

    Email(context.dstAddress, new InternetAddress(emailAddress),
      s"Great DANE Toolset Mail Test ($id)",
      s"""Hello,

         This is a test email sent by the Great DANE Toolset.

         At this point, we can tell you:
         - $certDescriptionForEmail


         ! Please reply to this email to complete the test !

         Then click here to followup and see the result:
            ${context.clickHost}/#/workflow/$id


         Also, you may click the following links to indicate if this email signature is valid or not.

           - Signature is Valid - ${context.clickHost}/workflow/$id/click/${ClickType.receivedSignedOk}?uiRedirect=true
           -
           - Signature is Invalid - ${context.clickHost}/workflow/$id/click/${ClickType.receivedSignedBad}?uiRedirect=true


         Regards,
         -- DST Team


         Certificate data:
         $certData}
     """.stripMargin
    )
  }
}


/** Factory object. */
object Workflow {
  def parseIdInSubject(subject: String): Option[String] = {
    val idInSubjectPattern = """\((\d*)\)""".r
    idInSubjectPattern.findFirstMatchIn(subject) match {
      case Some(m) => Option(m.group(1))
      case None => None
    }
  }
}
