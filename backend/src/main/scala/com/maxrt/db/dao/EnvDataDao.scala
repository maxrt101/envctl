package com.maxrt.db.dao

import com.maxrt.db.PrimaryKey
import com.maxrt.db.model.EnvData
import com.maxrt.db.dao.impl.DaoImpl
import com.maxrt.data.Reflection

class EnvDataDao extends DaoImpl[EnvData](
  Reflection.findAnnotationOrDie[EnvData, PrimaryKey].getPrimaryKeyName(),
  () => new EnvData)
