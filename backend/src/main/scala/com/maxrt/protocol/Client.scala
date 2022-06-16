package com.maxrt.protocol

import javax.websocket.Session

case class Client(clientType: Client.Type.Type, session: Session)

object Client {
  object Type extends Enumeration {
    type Type = Value

    val Controller, Webclient = Value
  }
}
