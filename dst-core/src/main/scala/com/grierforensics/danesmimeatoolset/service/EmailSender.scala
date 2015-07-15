package com.grierforensics.danesmimeatoolset.service

import java.util.Properties
import javax.mail._
import javax.mail.internet.MimeMessage

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.util.ConfigHolder.config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

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

    val authenticator: Authenticator {def getPasswordAuthentication: PasswordAuthentication} = new Authenticator() {
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
    //    message.setContent(email.content, email.contentType)
    email.content match {
      case p: Part if p.getContentType == "text/plain" => message.setContent(p.getContent, p.getContentType)
      case mbp: MimeBodyPart => {
        message.setContent(mbp.getContent, mbp.getContentType)
        addHeader(message, mbp, "Content-Disposition")
        addHeader(message, mbp, "Content-Description")
      }
      case mm: MimeMultipart => {
        message.setContent(mm)
      }
      case otherwise => {
        message.setContent(email.content, email.contentType)
        message.removeHeader("Content-Transfer-Encoding")  //these values come from the content setting above
        message.removeHeader("Content-Type")               //these values come from the content setting above
      }
    }
    message.saveChanges
    message
  }

  private def addHeader(message : MimeMessage, mimeBodyPart : MimeBodyPart, name : String) = {
      val headers = mimeBodyPart.getHeader(name)

      if (headers != null)
      {
          message.addHeader(name, headers(0))
      }
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
