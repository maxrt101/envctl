package com.maxrt.db

trait Model {
  def getField(name: String): Any
  def setField(name: String, value: AnyRef): Unit
}
