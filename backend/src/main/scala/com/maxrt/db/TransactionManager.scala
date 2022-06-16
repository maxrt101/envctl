package com.maxrt.db

import org.hibernate.Session

object TransactionManager {
  def run(session: Session, f: (Session) => Unit): Unit = {
    session.beginTransaction()
    f(session)
    session.getTransaction().commit()
    session.close()
  }
}
