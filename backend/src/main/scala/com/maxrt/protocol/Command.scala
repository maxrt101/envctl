package com.maxrt.protocol

import javax.websocket.Session
import org.json.{JSONArray, JSONObject}
import com.maxrt.WebSocket
import com.maxrt.db.Dao
import com.maxrt.db.model.*

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Command {

  def register(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.sessions.put(session.getId, sender)
    Protocol.clients.put(sender,
      Client(json.getJSONObject("data").getString("type") match {
        case "controller" => Client.Type.Controller
        case "client" => Client.Type.Webclient
        case _ => return Protocol.formError("Unknown client type")
      }, session))

    if (json.getJSONObject("data").getString("type").equals("controller")
        && Dao.controllerDao.getByField("name", sender).isEmpty) {
      val controller = new Controller
      controller.name = sender
      Dao.controllerDao.save(controller)
    }

    Protocol.formResponse("ok")
  }

  def unregister(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.clients.remove(sender)
    Protocol.sessions.remove(session.getId)
    Protocol.formResponse("ok")
  }

  def reregister(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.clients.remove(sender)
    Protocol.sessions.remove(session.getId)
    Protocol.sessions.put(session.getId, sender)

    if (Dao.controllerDao.getByField("name", sender).isEmpty) {
      val controller = new Controller
      controller.name = sender
      Dao.controllerDao.save(controller)
    } else {
      var controller = Dao.controllerDao.getByField("name", sender)(0)
      controller.name = sender
      Dao.controllerDao.update(controller)
    }

    Protocol.clients.put(sender,
      Client(json.getJSONObject("data").getString("type") match {
        case "controller" => Client.Type.Controller
        case "client" => Client.Type.Webclient
        case _ => return Protocol.formError("Unknown client type")
      }, session))

    Protocol.formResponse("ok")
  }

  def getControllers(sender: String, json: JSONObject, session: Session): JSONObject = {
    val data = new JSONObject()
    val list = new JSONArray()
    Dao.controllerDao.getAll().foreach(c => {
      val controllerJson = new JSONObject()
      controllerJson.put("name", c.name)
      controllerJson.put("state", if (Protocol.clients.contains(c.name)) "online" else "offline")
      list.put(controllerJson)
    })
    data.put("controllers", list)
    Protocol.formResponse("ok", data)
  }

  def getData(sender: String, json: JSONObject, session: Session): JSONObject = {
    val controllerName = json.getJSONObject("data").getString("controller")
    val controllers = Dao.controllerDao.getByField("name", controllerName)

    if (controllers.isEmpty) {
      return Protocol.formError("No such controlled")
    }

    var controller = controllers(0)
    var result = Dao.envDataDao.getByField("controllerId", controller.id)

    if (json.getJSONObject("data").has("time_range")) {
      val sdf = new SimpleDateFormat("yyyy:MM:dd HH-mm-ss")
      val from = new Timestamp(sdf.parse(json.getJSONObject("data").getJSONObject("time_range").getString("from")).getTime)
      val to = new Timestamp(sdf.parse(json.getJSONObject("data").getJSONObject("time_range").getString("to")).getTime)

      result = result.filter(x => x.timepoint.before(to) && x.timepoint.after(from))
    }

    val data = new JSONObject()
    val list = new JSONArray()
    result.foreach(x => list.put(x.toJson()))
    data.put("env_data", list)
    Protocol.formResponse("ok", data)
  }

  def updateData(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.clients.get(sender) match {
      case Some(client) =>
        client.clientType match {
          case Client.Type.Controller =>
            val controllers = Dao.controllerDao.getByField("name", sender)

            val envData = new EnvData
            envData.controllerId = if (controllers.isEmpty) 1 else controllers(0).id
            envData.temperature = json.getJSONObject("data").getFloat("temperature")
            envData.humidity = json.getJSONObject("data").getFloat("humidity")
            envData.timepoint = java.sql.Timestamp.valueOf(LocalDateTime.now())
            Dao.envDataDao.save(envData)

          case Client.Type.Webclient =>
            return Protocol.formError("update_data for this sender is unimplemented")
        }
      case None =>
        return Protocol.formError("No client registered for sender")
    }
    Protocol.formResponse("ok")
  }

  def updateName(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.checkTargetController(sender, json, session, (sender, target, json, session) => {
      target.session.getBasicRemote.sendText(json.toString)
      null
    })
  }

  def updateServer(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.checkTargetController(sender, json, session, (sender, target, json, session) => {
      target.session.getBasicRemote.sendText(json.toString)
      null
    })
  }

  def fanStart(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.checkTargetController(sender, json, session, (sender, target, json, session) => {
      if (1 to 100 contains json.getJSONObject("data").getInt("speed")) {
        target.session.getBasicRemote.sendText(json.toString)
        null
      } else {
        Protocol.formError("Speed out of range")
      }
    })
  }

  def fanStop(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.checkTargetController(sender, json, session, (sender, target, json, session) => {
      target.session.getBasicRemote.sendText(json.toString)
      null
    })
  }

  def fanRule(sender: String, json: JSONObject, session: Session): JSONObject = {
    Protocol.checkTargetController(sender, json, session, (sender, target, json, session) => {
      if (1 to 100 contains json.getJSONObject("data").getInt("speed")) {
        target.session.getBasicRemote.sendText(json.toString)
        null
      } else {
        Protocol.formError("Speed out of range")
      }
    })
  }

}
