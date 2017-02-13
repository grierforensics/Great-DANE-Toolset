// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.rest

import java.nio.file.{Paths, Files}

import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.webapp.WebAppContext

class TestServer(warPath: String, port: Int, context: String) {

  val server = new Server()

  val conn = new ServerConnector(server)
  conn.setHost("localhost")
  conn.setPort(port)
  server.addConnector(conn)

  val webapp = new WebAppContext()
  webapp.setContextPath(context)
  webapp.setWar(warPath)
  server.setHandler(webapp)

  server.start()

  def stop() {
    server.stop()
  }

  def url = s"http://${conn.getHost}:${conn.getPort}$context"
}

object TestServer {
  val projectRoot = if (Files.exists(Paths.get("dst-web"))) "dst-web/" else "./"

  val instance = new TestServer(projectRoot + "src/main/webapp", 63636, "")
}
