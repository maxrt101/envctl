package com.maxrt

import org.apache.log4j.BasicConfigurator
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer

import com.maxrt.db.SessionManager

object App {
  def main(args : Array[String]) = {
    BasicConfigurator.configure()

    val server = new Server()
    val connector = new ServerConnector(server)
    connector.setPort(8080)
    server.addConnector(connector)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")

    JavaxWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) => {
      wsContainer.setDefaultMaxTextMessageBufferSize(65535)
      wsContainer.setDefaultMaxSessionIdleTimeout(3600000)
      wsContainer.addEndpoint(classOf[WebSocket])
    })

    server.setHandler(context)

    SessionManager.openSession()
    server.start()
    WebSocket.logger.info("Server is running. Press CTRL+C to stop")
    server.join()
    SessionManager.closeSession()
  }
}
