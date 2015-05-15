package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}

object BackendMain extends App {

  val config = ConfigFactory.load("backend")
  val system = ActorSystem("backend", config)

  system.actorOf(Props[BoxOffice], "boxOffice")

}
