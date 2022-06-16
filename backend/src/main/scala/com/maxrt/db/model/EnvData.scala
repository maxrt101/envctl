package com.maxrt.db.model

import com.maxrt.db.{Model, PrimaryKey}
import org.json.JSONObject

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "env_data")
@PrimaryKey("id")
class EnvData extends Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int = 0
  var controllerId: Int = 0
  var timepoint: java.sql.Timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now())
  var temperature: Float = 0
  var humidity: Float = 0

  def getField(name: String): Any = name match {
    case "id"           => id
    case "controllerId" => controllerId
    case "timepoint"    => timepoint
    case "temperature"  => temperature
    case "humidity"     => humidity
  }

  def setField(name: String, value: AnyRef): Unit = name match {
    case "id"           => id = value.asInstanceOf[Int]
    case "controllerId" => controllerId = value.asInstanceOf[Int]
    case "timepoint"    => timepoint = value.asInstanceOf[java.sql.Timestamp]
    case "temparature"  => temperature = value.asInstanceOf[Float]
    case "humidity"     => humidity = value.asInstanceOf[Float]
  }

  override def toString() =
    s"EnvData[id=$id, controllerId=$controllerId, time=$timepoint, temperature=$temperature, humidity=$humidity]"

  def toJson(): JSONObject = {
    val json = new JSONObject
    json.put("id", id)
    json.put("controllerId", controllerId)
    json.put("timepoint", timepoint)
    json.put("temperature", temperature)
    json.put("humidity", humidity)
    json
  }

}
