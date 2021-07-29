package com.example.services

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

final case class Proxy(host: String, port: Int)

class ProxyService(implicit val system: ActorSystem[_]) {

  implicit val executionContext = system.executionContext

  lazy val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt"))

  def getProxies(): Future[List[Proxy]] = for {
    source <- responseFuture.map(response => response.entity.dataBytes)
    rawProxies <- streamMigrationFile(source)
  } yield getPureProxies(rawProxies)

  private def streamMigrationFile(source: Source[ByteString, _]): Future[String] = {
    val sink = Sink.fold[String, ByteString]("") { case (acc, str) =>
      acc + str.decodeString("US-ASCII")
    }
    source.runWith(sink)
  }

  private def getPureProxies(rawProxies: String): List[Proxy] =
    rawProxies.split("\n").map { x =>
      val rawProxy = x.split(":")
      Proxy(rawProxy.head, rawProxy.last.toInt)
    }.toList
}
