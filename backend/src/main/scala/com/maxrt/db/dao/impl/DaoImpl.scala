package com.maxrt.db.dao.impl

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.*
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import collection.convert.ImplicitConversions._

import javax.persistence.{EntityManager, EntityManagerFactory, Persistence}

import org.hibernate.Session
import org.hibernate.query.Query

import com.maxrt.db.{Dao, Model, SessionManager, TransactionManager}
import com.maxrt.data.Reflection

class DaoImpl[T <: Model](primaryKeyName: String, creator: () => T)(implicit ct: ClassTag[T]) extends Dao[T] {
  def get(id: IdType): Option[T] =
    Option(SessionManager.getSession().createQuery(s"FROM ${ct.runtimeClass.getSimpleName} WHERE $primaryKeyName = :id").setParameter("id", id).uniqueResult().asInstanceOf[T])

  def getByField[F](fieldName: String, fieldValue: F): List[T] =
    SessionManager.getSession().createQuery(s"FROM ${ct.runtimeClass.getSimpleName} WHERE $fieldName = :value").setParameter("value", fieldValue).list().toList.map(x => x.asInstanceOf[T])

  def getAll(): List[T] =
    SessionManager.getSession().createQuery(s"FROM ${ct.runtimeClass.getSimpleName}").list().toList.map(x => x.asInstanceOf[T])

  def save(value: T): Unit =
    TransactionManager.run(SessionManager.getSession(), (session: Session) => session.save(value))

  def update(value: T): Unit =
    TransactionManager.run(SessionManager.getSession(), (session: Session) => session.saveOrUpdate(value))

  def delete(value: T): Unit =
    TransactionManager.run(SessionManager.getSession(), (session: Session) =>
      session.createQuery(s"DELETE ${ct.runtimeClass.getSimpleName} WHERE $primaryKeyName = :id").setParameter("id", value.getField(primaryKeyName)).executeUpdate())

  def getModelClassTag(): ClassTag[T] = ct

  def newModelInstance(): T = creator()
}
