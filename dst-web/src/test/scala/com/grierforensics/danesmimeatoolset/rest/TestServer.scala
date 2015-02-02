package com.grierforensics.danesmimeatoolset.rest

import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.webapp.WebAppContext

class TestServer(war: String, port: Int, context: String) {

  val server = new Server()

  val conn = new ServerConnector(server)
  conn.setHost("localhost")
  conn.setPort(port)
  server.addConnector(conn)

  val webapp = new WebAppContext()
  webapp.setContextPath(context)
  webapp.setWar(war)
  server.setHandler(webapp)

  server.start()

  def stop() {
    server.stop()
  }
}
