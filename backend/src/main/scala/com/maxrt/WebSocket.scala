package com.maxrt

import java.io.IOException
import javax.websocket.{CloseReason, OnClose, OnError, OnMessage, OnOpen, Session}
import javax.websocket.server.ServerEndpoint
import org.json.JSONObject
import org.apache.log4j.Logger
import com.maxrt.protocol.Protocol

@ServerEndpoint("/api")
class WebSocket {
  @OnOpen
  def onOpen(session: Session): Unit = {
    WebSocket.logger.info(s"[ws] (${session.getId()}) New connection")
    Protocol.sessions.put(session.getId, "")
  }

  @OnClose
  def onClose(reason: CloseReason, session: Session): Unit = {
    WebSocket.logger.info(s"[ws] (${session.getId()}) Closed connection")
    Protocol.sessions.get(session.getId) match {
      case Some(senderName) => Protocol.clients.remove(senderName)
      case None => WebSocket.logger.error(s"[ws] No client record for session id ${session.getId}")
    }
    Protocol.sessions.remove(session.getId)
  }

  @OnError
  def onError(session: Session, throwable: Throwable): Unit = {
    WebSocket.logger.info(s"[ws] (${session.getId()}) Error: ${throwable.toString}")
    Protocol.sessions.get(session.getId) match {
      case Some(senderName) => Protocol.clients.remove(senderName)
      case None => WebSocket.logger.error(s"[ws] No client record for session id ${session.getId}")
    }
    Protocol.sessions.remove(session.getId)
  }

  @OnMessage
  def onMessage(text: String, session: Session): Unit = session.getBasicRemote().sendText((
    try {
      WebSocket.logger.info(s"[ws] (${session.getId()}) New message '${text}'")

      val json = new JSONObject(text);

      if (json.has("command")) {
        Protocol.handleCommand(session, json)
      } else {
        Protocol.formError("Not a command")
      }
    } catch {
      case e: Exception =>
        WebSocket.logger.error(s"[ws] (${session.getId()}) Exception during message processing: ${e.toString}")
        Protocol.formError("Exception")
    }
  ).toString)
}

object WebSocket {
  val logger = Logger.getLogger(classOf[WebSocket])
}
