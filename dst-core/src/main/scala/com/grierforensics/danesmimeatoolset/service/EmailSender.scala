package com.grierforensics.danesmimeatoolset.service

import java.util.Properties
import javax.mail._
import javax.mail.internet.MimeMessage

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.util.ConfigHolder.config
import com.typesafe.scalalogging.LazyLogging


class EmailSender(val smtpHost: String, val username: String, val password: String, val useTls: Boolean = true) extends LazyLogging {

  val session: Session = {
    val props: Properties = System.getProperties
    props.put("mail.smtp.host", smtpHost);
    if (useTls) {
      props.put("mail.smtp.port", "587");
      props.put("mail.smtp.starttls.enable", "true");
    }
    props.put("mail.smtp.auth", "true");
    val authenticator: Authenticator {def getPasswordAuthentication: PasswordAuthentication} = new Authenticator() {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(username, password)
      }
    }
    Session.getDefaultInstance(props, authenticator)
  }

  def send(email: Email): Unit = {
    val message: MimeMessage = createMessage(email)
    try {
      Transport.send(message)
      logger.debug(s"email sent to=${email.to}")
    }
    catch {
      case e: Exception => throw new EmailSendFailedException(e)
    }
  }

  def createMessage(email: Email): MimeMessage = {
    val message: MimeMessage = new MimeMessage(session)
    message.setFrom(email.from)
    message.setReplyTo(Array(email.from))
    message.setRecipient(Message.RecipientType.TO, email.to)
    message.setSubject(email.subject)
    //    message.setContent(email.content, email.contentType)
    email.content match {
      case p: Part if p.getContentType == "text/plain" => message.setContent(p.getContent, p.getContentType)
      case otherwise => message.setContent(email.content, email.contentType)
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

object EmailSender extends EmailSender(
  config.getString("EmailSender.host"),
  config.getString("EmailSender.username"),
  config.getString("EmailSender.password"),
  true)

class EmailSendFailedException(cause:Throwable) extends Exception(cause)
