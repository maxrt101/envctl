package com.maxrt.db

import org.hibernate.{Session, SessionFactory, HibernateException}
import org.hibernate.cfg.Configuration

object SessionManager {
  private var sessionFactory: SessionFactory = null

  def openSession() = sessionFactory = new Configuration().configure().buildSessionFactory()
  def closeSession() = sessionFactory.close()
  def getSession() = sessionFactory.openSession()
}
