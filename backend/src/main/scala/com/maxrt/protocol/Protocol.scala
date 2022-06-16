package com.maxrt.protocol

import javax.websocket.Session
import scala.collection.JavaConverters._
import scala.collection.mutable.Map
import org.json.{JSONArray, JSONObject}
import com.maxrt.WebSocket

object Protocol {
  val clients = Map.empty[String, Client] // [sender_name, (type, session)]
  var sessions = Map.empty[String, String] // [session_id, sender_name]

  val commands = Map(
    "register"        -> Command.register,
    "unregister"      -> Command.unregister,
    "reregister"      -> Command.reregister,
    "get_controllers" -> Command.getControllers,
    "get_data"        -> Command.getData,
    "update_data"     -> Command.updateData,
    "update_name"     -> Command.updateName,
    "update_server"   -> Command.updateServer,
    "fan_start"       -> Command.fanStart,
    "fan_stop"        -> Command.fanStop,
    "fan_rule"        -> Command.fanRule,
    "ping"            -> Command.ping
  )

  def formResponse(responseStatus: String, data: JSONObject = null): JSONObject = {
    val result = new JSONObject()
    result.put("status", responseStatus)
    if (data != null)
      result.put("data", data)
    result
  }

  def formError(message: String): JSONObject = {
    val result = formResponse("error")
    result.put("message", message)
    result
  }

  def checkTargetController(sender: String, json: JSONObject, session: Session, fn: (String, Client, JSONObject, Session) => JSONObject): JSONObject = {
    Protocol.clients.get(sender) match {
      case Some(client) =>
        client.clientType match {
          case Client.Type.Webclient =>
            val controller = json.getJSONObject("data").getString("controller")
            Protocol.clients.get(controller) match {
              case Some(targetClient) =>
                val returnJson = fn(sender, targetClient, json, session)
                if (returnJson != null) {
                  return returnJson
                }
              case None =>
                return Protocol.formError("No such controller")
            }
          case Client.Type.Controller =>
            return Protocol.formError("Invalid sender")
        }
      case None =>
        return Protocol.formError("No client registered for sender")
    }
    Protocol.formResponse("ok")
  }

  def handleCommand(session: Session, json: JSONObject): JSONObject = {
    val sender = json.getString("sender")
    commands.getOrElse(json.getString("command"), (sender: String, json: JSONObject, session: Session) => formError("Unknown command"))(sender, json, session);
  }
}
