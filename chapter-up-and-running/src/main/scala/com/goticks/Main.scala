package com.goticks

import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http

import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Main extends App {
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("goticks")


  implicit val executionContext = system.dispatcher

  implicit val requestTimeout: Timeout = {
    val d = Duration(system.settings.config.getString("spray.can.server.request-timeout"))
    FiniteDuration(d.length, d.unit) - 1.second
  }

  val api = system.actorOf(Props(new RestApi()), "httpInterface")

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }
}
