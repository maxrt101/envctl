package com.maxrt.protocol

import javax.websocket.Session
import scala.collection.JavaConverters._
import scala.collection.mutable.Map
import org.json.{JSONArray, JSONObject}
import com.maxrt.WebSocket

object Protocol {
  val clients = Map.empty[String, Client] // [sender_name, (type, session)]
  var sessions = Map.empty[String, String] // [session_id, sender_name]

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
    json.getString("command") match {
      case "register" => Command.register(sender, json, session)
      case "unregister" => Command.unregister(sender, json, session)
      case "reregister" => Command.reregister(sender, json, session)
      case "get_controllers" => Command.getControllers(sender, json, session)
      case "get_data" => Command.getData(sender, json, session)
      case "update_data" => Command.updateData(sender, json, session)
      case "update_name" => Command.updateName(sender, json, session)
      case "update_server" => Command.updateServer(sender, json, session)
      case "fan_start" => Command.fanStart(sender, json, session)
      case "fan_stop" => Command.fanStop(sender, json, session)
      case "fan_rule" => Command.fanRule(sender, json, session)
      case _ =>
        formError("Unknown command")
    }
  }
}
