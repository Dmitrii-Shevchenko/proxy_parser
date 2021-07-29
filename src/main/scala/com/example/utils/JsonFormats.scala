package com.example.utils

import com.example.services.UserRegistry.ActionPerformed
import com.example.services.{Proxy, User, Users}
import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol  {

  implicit val proxyJsonFormat = jsonFormat2(Proxy)
  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
