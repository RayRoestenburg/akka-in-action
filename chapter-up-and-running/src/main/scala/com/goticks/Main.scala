package com.goticks

import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can.Http

import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load() //<co id="configure"/>
  val host = config.getString("http.host") //<co id="gethttppars"/>
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("goticks")

  implicit val requestTimeout: Timeout = { // <co id="timeout_spray_can"/>
    val t = system.settings
      .config
      .getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }

  val api = system.actorOf(Props(new RestApi()), "httpInterface") //<co id="toplevelactor"/>

  implicit val executionContext = system.dispatcher

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port)) //<co id="startServer"/>
    .mapTo[Http.Event] //<co id="response"/>
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }
}
