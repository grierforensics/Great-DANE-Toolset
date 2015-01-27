package com.grierforensics.danesmimeatoolset

import java.util.Properties
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}


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

    val fromUser: Address = new InternetAddress(formatAddress(email.fromFullName, email.fromEmailAddress))
    val toUser: Address = new InternetAddress(formatAddress(email.toFullName, email.toEmailAddress))

    val message: MimeMessage = new MimeMessage(session)
    message.setFrom(fromUser)
    message.setRecipient(Message.RecipientType.TO, toUser)
    message.setSubject(email.subject)
    message.setContent(email.message, email.contentType)
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

object EmailSender extends EmailSender("smtp.gmail.com", "test1.dst@example.com", "test1.dst!", true)
//todo: move this static config to config
