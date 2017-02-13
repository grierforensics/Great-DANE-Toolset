// Copyright (C) 2017 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.service

import java.util.Properties
import javax.mail._
import javax.mail.internet.{MimeMessage, MimeBodyPart, MimeMultipart}

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.util.ConfigHolder.config
import com.typesafe.scalalogging.LazyLogging

/** Sends Email's via SMTP server. */
class EmailSender(val smtpHost: String, val username: String, val password: String,
                  val useTls: Boolean, val port: Int) extends LazyLogging {

  private val session: Session = {
    val props: Properties = System.getProperties
    props.put("mail.smtp.host", smtpHost)
    props.put("mail.smtp.port", port.toString)
    props.put("mail.smtp.auth", "true")
    if (useTls) {
      props.put("mail.smtp.starttls.enable", "true")
      props.put("mail.smtp.ssl.checkserveridentity", "false")
      props.put("mail.smtp.ssl.trust", "*")
    }

    val authenticator: Authenticator = new Authenticator() {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(username, password)
      }
    }

    Session.getDefaultInstance(props, authenticator)
  }

  /** Sends an email. */
  def send(email: Email): Unit = {
    val message: MimeMessage = createMessage(email)
    try {
      Transport.send(message)
      logger.debug(s"email sent to=${email.to}")
    }
    catch {
      case e: SendFailedException => throw e
      case e: Exception => throw new EmailSendFailedException(e)
    }
  }

  /** Utility method for creating a MimeMessage from an Email instance. */
  def createMessage(email: Email): MimeMessage = {
    val message: MimeMessage = new MimeMessage(session)
    message.setFrom(email.from)
    message.setReplyTo(Array(email.from))
    message.setRecipient(Message.RecipientType.TO, email.to)
    message.setSubject(email.subject)
    email.content match {
      case p: Part if p.getContentType == "text/plain" => {
        message.setContent(p.getContent, p.getContentType)
      }
      case mbp: MimeBodyPart => {
        message.setContent(mbp.getContent, mbp.getContentType)
        if (mbp.getContentType == "application/pkcs7-mime") {
          message.addHeader("Content-Disposition", mbp.getHeader("Content-Disposition")(0))
          message.addHeader("Content-Description", mbp.getHeader("Content-Description")(0))
        }
      }
      case mmp: MimeMultipart => {
        message.setContent(mmp)
      }
    }
    message.saveChanges
    message
  }

  private def formatAddress(name: Option[String], address: String): String = {
    name match {
      case Some(s) => "\"" + s + "\"<" + address + ">"
      case None => address
    }
  }
}

/** Singleton instance. */
object EmailSender extends EmailSender(
  config.getString("EmailSender.host"),
  config.getString("EmailSender.username"),
  config.getString("EmailSender.password"),
  config.getBoolean("EmailSender.useTls"),
  config.getInt("EmailSender.port"))

/** Thrown when email can not be sent. */
class EmailSendFailedException(cause: Throwable) extends Exception(cause)
