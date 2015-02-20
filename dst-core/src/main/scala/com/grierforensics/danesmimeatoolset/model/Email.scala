package com.grierforensics.danesmimeatoolset.model

import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMultipart}

class Email private(val from: InternetAddress,
                    val to: InternetAddress,
                    val subject: String,
                    val content: AnyRef) {

  content match {
    case message: MimeMultipart => message.getContentType
    case message: MimeBodyPart => message.getContentType
    case _ => throw new IllegalArgumentException("email message must be a non-null MimeMultipart or MimeBodyPart")
  }

  def bodyPart: MimeBodyPart = content match {
    case message: MimeMultipart => throw new NotImplementedError("wrapping MimeMultipart in MimeBodyPart not implemented yet") //todo: implement
    case message: MimeBodyPart => message
  }

  def multipart: MimeMultipart = content match {
    case message: MimeMultipart => message
    case message: MimeBodyPart => throw new NotImplementedError("wrapping MimeBodyPart in MimeMultipart not implemented yet") //todo: implement
  }

  def contentType(): String = content match {
    case message: MimeMultipart => message.getContentType
    case message: MimeBodyPart => message.getContentType
  }
}

object Email {

  /**
   * Constructs an Email using a MimeBodyPart message
   */
  def apply(from: InternetAddress, to: InternetAddress, subject: String, message: MimeMultipart): Email = {
    new Email(from, to, subject, message)
  }

  /**
   * Constructs an Email using a MimeMultipart message
   */
  def apply(from: InternetAddress, to: InternetAddress, subject: String, message: MimeBodyPart): Email = {
    new Email(from, to, subject, message)
  }

  /**
   * Constructs an Email using a text String
   */
  def apply(from: InternetAddress, to: InternetAddress, subject: String, text: String): Email = {
    new Email(from, to, subject, {
      val m = new MimeBodyPart()
      m.setContent(text,"text/plain")
//      m.setText(text)
      m
    })
  }
}
