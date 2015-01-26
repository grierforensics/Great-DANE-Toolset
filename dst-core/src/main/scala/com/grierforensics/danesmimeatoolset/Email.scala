package com.grierforensics.danesmimeatoolset

import javax.mail.internet.{MimeBodyPart, MimeMultipart}

class Email private(val fromFullName: Option[String], val fromEmailAddress: String,
                    val toFullName: Option[String], val toEmailAddress: String,
                    val subject: String, val message: AnyRef) {

  message match {
    case message: MimeMultipart => message.getContentType
    case message: MimeBodyPart => message.getContentType
    case _ => throw new IllegalArgumentException("email message must be a non-null MimeMultipart or MimeBodyPart")
  }

  def bodyPart: MimeBodyPart = message match {
    case message: MimeMultipart => throw new NotImplementedError("wrapping MimeMultipart in MimeBodyPart not implemented yet") //todo: implement
    case message: MimeBodyPart => message
  }

  def multipart: MimeMultipart = message match {
    case message: MimeMultipart => message
    case message: MimeBodyPart => throw new NotImplementedError("wrapping MimeBodyPart in MimeMultipart not implemented yet") //todo: implement
  }

  def contentType(): String = message match {
    case message: MimeMultipart => message.getContentType
    case message: MimeBodyPart => message.getContentType
  }
}

object Email {

  /**
   * Constructs an Email using a MimeBodyPart message
   */
  def apply(fromFullName: Option[String], fromEmailAddress: String,
            toFullName: Option[String], toEmailAddress: String,
            subject: String, message: MimeMultipart): Email = {
    new Email(fromFullName, fromEmailAddress, toFullName, toEmailAddress, subject, message)
  }

  /**
   * Constructs an Email using a MimeMultipart message
   */
  def apply(fromFullName: Option[String], fromEmailAddress: String,
            toFullName: Option[String], toEmailAddress: String,
            subject: String, message: MimeBodyPart): Email = {
    new Email(fromFullName, fromEmailAddress, toFullName, toEmailAddress, subject, message)
  }

  /**
   * Constructs an Email using a text String
   */
  def apply(fromFullName: Option[String], fromEmailAddress: String,
            toFullName: Option[String], toEmailAddress: String,
            subject: String, text: String): Email = {
    new Email(fromFullName, fromEmailAddress, toFullName, toEmailAddress, subject, {
      val m = new MimeBodyPart();
      m.setText(text)
      m
    })
  }
}
