package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import spray.can.Http.Bind
import spray.can.Http

object SingleNodeMain extends App {

  val config = ConfigFactory.load("singlenode")

  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val system = ActorSystem("singlenode", config)

  val restInterface = system.actorOf(Props[RestInterface],
                                     "restInterface")
  Http(system).manager ! Bind(listener = restInterface,
                              interface = host,
                              port=port)

}
