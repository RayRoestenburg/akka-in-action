package com.goticks


import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer

import com.typesafe.config.{ Config, ConfigFactory }
import scala.util.{ Failure, Success }




object Main extends App
    with RequestTimeout {

  val config = ConfigFactory.load()
  val host = config.getString("http.host") // 設定からホスト名とポートを取得
  val port = config.getInt("http.port")

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher  // bindAndHandleは暗黙のExecutionContextが必要

  val api = new RestApi(system, requestTimeout(config)).routes // the RestApi provides a Route

  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) // HTTPサーバーの起動

  val log =  Logging(system.eventStream, "go-ticks")
  bindingFuture.map { serverBinding =>
    log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onComplete {
    case Success(_) =>
      log.info("Success to bind to {}:{}", host, port)
    case Failure(ex) =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}




trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}


