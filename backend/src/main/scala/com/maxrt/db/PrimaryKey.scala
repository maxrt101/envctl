package com.maxrt.db

/**
 * Indicates what field is primary key
 * @param primaryKeyName - Primary Key Name of the Model's table
 */
class PrimaryKey(primaryKeyName: String) extends scala.annotation.StaticAnnotation {
  def getPrimaryKeyName() = primaryKeyName
}
