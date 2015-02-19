package com.grierforensics.danesmimeatoolset.service

import java.util.Properties
import javax.mail._

import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.sun.mail.pop3.{POP3SSLStore, POP3Store}


class EmailFetcher(val pop3Host: String, val username: String, val password: String, val folderName: String = "INBOX") {

  def fetchAndDelete(handle: Message => Boolean) {
    val pop3Port: Int = 995

    val pop3Props = new Properties()
    pop3Props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false")
    pop3Props.setProperty("mail.pop3.port",  "995")
    pop3Props.setProperty("mail.pop3.socketFactory.port", "995")

    val url = new URLName("pop3", pop3Host, pop3Port, "", username, password)

    val session = Session.getInstance(pop3Props, null)
    val store = new POP3SSLStore(session, url)
    store.connect()
    
    try {
      val folder: Folder = store.getFolder(folderName)
      folder.open(Folder.READ_WRITE);
      val messages: Array[Message] = folder.getMessages
      for (message <- messages) {
        if(handle(message))
        message.setFlag(Flags.Flag.DELETED, true);
      }
      folder.close(true)
    }
    finally {
      if (store.isConnected)
        store.close();
    }
  }

  //  private class MessageHandler {
  //    private final val toolkit: SMIMEToolkit = null
  //    private final val myIdentity: JcaPKIXIdentity = null
  //    private final val toUserCert: X509Certificate = null
  //
  //    def this(toolkit: SMIMEToolkit, myIdentity: JcaPKIXIdentity, toUserCert: X509Certificate) {
  //      this()
  //      this.toolkit = toolkit
  //      this.myIdentity = myIdentity
  //      this.toUserCert = toUserCert
  //    }
  //
  //    @throws(classOf[Exception])
  //    def handleMessage(i: Int, message: Message) {
  //      val fromAddress: InternetAddress = message.getFrom(0).asInstanceOf[InternetAddress]
  //      if (fromAddress.getAddress.equalsIgnoreCase("user@example.com")) {
  //        val details: DaneSmimeTool.MessageDetails = getMessageDetails(toolkit, message, myIdentity, toUserCert)
  //        System.out.println("---------------------------------")
  //        System.out.println("Email Number " + (i + 1))
  //        System.out.println("Valid Signature  : " + details.isSignatureValid)
  //        System.out.println("Is DANE certified: " + details.isDANESigned)
  //        System.out.println("Is CA certified  : " + details.isCASigned)
  //      }
  //    }
  //  }

}


object EmailFetcher extends EmailFetcher(
  config.getString("EmailFetcher.host"),
  config.getString("EmailFetcher.username"),
  config.getString("EmailFetcher.password"),
  "INBOX")

