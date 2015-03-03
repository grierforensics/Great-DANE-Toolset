package com.grierforensics.danesmimeatoolset.service

import java.util.{Properties, Timer, TimerTask}
import javax.mail._
import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import com.sun.mail.pop3.POP3SSLStore
import com.typesafe.scalalogging.LazyLogging


class EmailFetcher(val pop3Host: String,
                   val username: String,
                   val password: String,
                   val folderName: String = "INBOX",
                   val personal: String = "DANE SMIMEA Toolset") extends LazyLogging {
  val fetchLock = new Object
  val asyncLock = new Object
  var asyncTimer: Timer = null
  @volatile var fetchCount = 0L

  val WAIT_POLLING_PERIOD: Long = 100

  def fetchAndDelete(handler: Message => Boolean) {
    fetchLock.synchronized {
      logger.debug("fetching email")
      val pop3Port: Int = 995

      val pop3Props = new Properties()
      pop3Props.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
      pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false")
      pop3Props.setProperty("mail.pop3.port", pop3Port.toString)
      pop3Props.setProperty("mail.pop3.socketFactory.port", pop3Port.toString)
      val session = Session.getInstance(pop3Props, null)

      val url = new URLName("pop3", pop3Host, pop3Port, "", username, password)
      val store = new POP3SSLStore(session, url)
      store.connect()

      var handledCount = 0
      try {
        val folder: Folder = store.getFolder(folderName)
        folder.open(Folder.READ_WRITE)
        val messages: Array[Message] = folder.getMessages
        for (message <- messages) {
          if (handler(message)) {
            message.setFlag(Flags.Flag.DELETED, true)
            handledCount += 1
          }
        }
        folder.close(true)
      }
      finally {
        if (store.isConnected)
          store.close()
        fetchCount += 1
        logger.debug(s"fetched email handledCount=$handledCount fetchCount=$fetchCount")
      }
    }
  }

  /**
   * Waits for the given number of email fetches or the timeout or for the asyncIsFetching == false
   * @param fetches number of fetches to wait for.  Default is 1
   * @param timeout millis before timeout.  0 or less means no timeout.  Default is -1
   */
  def waitForFetch(fetches: Int = 1, timeout: Long = -1) = {
    val targetCount: Long = fetchCount + fetches
    val until: Long = if (timeout > 0) System.currentTimeMillis() + timeout else Long.MaxValue
    var now: Long = System.currentTimeMillis()
    while (asyncIsFetching && fetchCount < targetCount && now < until) {
      Thread.sleep(Math.min(WAIT_POLLING_PERIOD, until - now))
      now = System.currentTimeMillis()
    }
  }


  def asyncFetchAndDelete(handler: Message => Boolean, period: Long = -1): Unit = {
    asyncLock.synchronized {
      asyncStop()
      asyncTimer = new Timer(true)

      if (period > 0)
        asyncTimer.schedule(new TimerTask {
          override def run(): Unit = {
            try {
              fetchAndDelete(handler)
            }
            catch {
              case e: Exception => logger.warn("Email fetch failed.", e)
            }
          }
        }, 0, period)
      else
        asyncTimer.schedule(new TimerTask {
          override def run(): Unit = {
            try {
              fetchAndDelete(handler)
            }
            catch {
              case e: Exception => logger.warn("Email fetch failed.", e)
            }
            asyncStop() //mark this as stopped because no periodic tasks are coming
          }
        }, 0)
    }
  }

  def asyncIsFetching: Boolean = {
    asyncLock.synchronized {
      asyncTimer != null
    }
  }

  def asyncStop(): Unit = {
    asyncLock.synchronized {
      if (asyncTimer != null) {
        asyncTimer.cancel()
        asyncTimer = null
      }
    }
  }

  def address: InternetAddress = new InternetAddress(username, personal)
}


object EmailFetcher extends EmailFetcher(
  config.getString("EmailFetcher.host"),
  config.getString("EmailFetcher.username"),
  config.getString("EmailFetcher.password"),
  "INBOX",
  "DANE SMIMEA Toolset"
)

