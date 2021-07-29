package com.example.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.services.ProxyService
import com.example.utils.JsonFormats

class ProxyController(proxyService: ProxyService) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  val route: Route =
    pathPrefix("proxies") {
        pathEnd {
            get {
              onSuccess(proxyService.getProxies()) { proxies =>
                  complete((StatusCodes.OK, proxies))
              }
            }
        }
    }
}
