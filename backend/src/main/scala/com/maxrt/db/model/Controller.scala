package com.maxrt.db.model

import com.maxrt.db.{Model, PrimaryKey}
import javax.persistence.*

@Entity
@Table(name = "controller")
@PrimaryKey("id")
class Controller extends Model {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int = 0
  var name: String = ""

  def getField(name: String): Any = name match {
    case "id"        => id
    case "name"      => this.name
  }

  def setField(name: String, value: AnyRef): Unit = name match {
    case "id"        => id = value.asInstanceOf[Int]
    case "name"      => this.name = value.asInstanceOf[String]
  }

  override def toString() = s"Controller[id=$id, name=$name]"

}
