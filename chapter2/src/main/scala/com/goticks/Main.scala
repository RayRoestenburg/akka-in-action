package com.goticks

import spray.can.server.SprayCanHttpServerApp
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object Main extends App with SprayCanHttpServerApp {
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val api = system.actorOf(Props(new RestInterface()), "httpInterface")
  newHttpServer(api) ! Bind(interface = host, port = port)
}
