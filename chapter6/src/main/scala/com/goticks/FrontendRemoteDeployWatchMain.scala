package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}
import spray.can.Http
import spray.can.Http.Bind

object FrontendRemoteDeployWatchMain extends App {
  val config = ConfigFactory.load("frontend-remote-deploy")

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val system = ActorSystem("frontend", config)

  class RestInterfaceWatch extends RestInterface
                           with ConfiguredRemoteBoxOfficeDeployment

  val restInterface = system.actorOf(Props[RestInterfaceWatch],
  "restInterface")

  Http(system).manager ! Bind(listener = restInterface,
  interface = host,
  port =port)

}

