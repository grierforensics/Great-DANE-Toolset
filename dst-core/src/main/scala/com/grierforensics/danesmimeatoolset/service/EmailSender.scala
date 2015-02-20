package com.grierforensics.danesmimeatoolset.service

import java.util.Properties
import javax.mail._
import javax.mail.internet.MimeMessage

import com.grierforensics.danesmimeatoolset.model.Email
import com.grierforensics.danesmimeatoolset.util.ConfigHolder.config


class EmailSender(val smtpHost: String, val username: String, val password: String, val useTls: Boolean = true) {

  def send(email: Email): Unit = {

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
    val session: Session = Session.getDefaultInstance(props, authenticator)

    val message: MimeMessage = new MimeMessage(session)
    message.setFrom(email.from)
    message.setRecipient(Message.RecipientType.TO, email.to)
    message.setSubject(email.subject)
    //    message.setContent(email.content, email.contentType)
    email.content match {
      case p: Part if p.getContentType == "text/plain" => message.setContent(p.getContent, p.getContentType)
      case otherwise => message.setContent(email.content, email.contentType)
    }
    message.saveChanges

    Transport.send(message);
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
