package com.maxrt.db

import com.maxrt.db.dao.*

import scala.reflect.ClassTag

trait Dao[T <: Model] {
  type IdType = Int
  type ModelType = T

  def get(id: IdType): Option[T]
  def getByField[F](fieldName: String, fieldValue: F): List[T]
  def getAll(): List[T]
  def save(value: T): Unit
  def update(value: T): Unit
  def delete(value: T): Unit
  def getModelClassTag(): ClassTag[T]
  def newModelInstance(): T
}

object Dao {
  val controllerDao = new ControllerDao
  val envDataDao = new EnvDataDao
}
