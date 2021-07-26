package com.example.services

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

class ProxyService(implicit val system: ActorSystem[_]) {

  implicit val executionContext = system.executionContext

  lazy val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt"))
  def getProxies(): Future[String] = responseFuture.map(x => x.entity).map(x => x.dataBytes).flatMap(y => streamMigrationFile(y))

  def streamMigrationFile(source: Source[ByteString, _]): Future[String] = {
    val sink = Sink.fold[String, ByteString]("") { case (acc, str) =>
      acc + str.decodeString("US-ASCII")
    }
    source.runWith(sink)
  }
}
