package com.grierforensics.danesmimeatoolset.service

import javax.mail.Message
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.model.Workflow
import com.grierforensics.danesmimeatoolset.persist.WorkflowDao
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.grierforensics.danesmimeatoolset.util.IdentityUtil
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.util.encoders.Base64

/** Context sets up services according to config.
  *
  * This is in place of dependency injection for now.  Future development
  * can either make this more robust, or move to full fledged DI. */
object Context extends LazyLogging {
  val daneSmimeaService = new DaneSmimeaService(config.getString("DaneSmimeaService.dns"))

  val sender = EmailSender
  val fetcher = EmailFetcher
  val workflowDao = WorkflowDao

  val dstAddress: InternetAddress = new InternetAddress(config.getString("Workflow.fromAddress"), config.getString("Workflow.fromName"))
  val dstIdentity = IdentityUtil.loadIdentity("danesmime-test-priv.pem","danesmime-test-cert.pem")
//  val dstIdentity = IdentityUtil.generateIdentity(dstAddress)
  val dstCertBase64Str = Base64.toBase64String(new X509CertificateHolder(dstIdentity.getX509Certificate.getEncoded).getEncoded)

  val clickHost = config.getString("Workflow.clickHostUrl")

  def fetcherStart() = fetcher.asyncFetchAndDelete(handleMessage, config.getLong("EmailFetcher.period"))

  def fetcherStop() = fetcher.asyncStop()

  // Private

  private def handleMessage(message: Message): Boolean = {
    val id: Option[String] = Workflow.parseIdInSubject(message.getSubject)
    if (id.isEmpty)
      return false //ignore non workflow emails

    workflowDao.fetch(id.get) match {
      case Some(w) => w.handleMessage(message)
      case None => logger.info("Dumping email for unknown/expired workflow id:" + id.get)
    }

    true
  }

}
