package com.maxrt.db.dao

import com.maxrt.db.PrimaryKey
import com.maxrt.db.model.Controller
import com.maxrt.db.dao.impl.DaoImpl
import com.maxrt.data.Reflection

class ControllerDao extends DaoImpl[Controller](
  Reflection.findAnnotationOrDie[Controller, PrimaryKey].getPrimaryKeyName(),
  () => new Controller)
