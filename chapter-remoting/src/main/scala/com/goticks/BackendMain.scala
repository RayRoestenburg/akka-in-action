package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}

object BackendMain extends App with RequestTimeout {
  val config = ConfigFactory.load("backend")
  val system = ActorSystem("backend", config)
  implicit val requestTimeout = configuredRequestTimeout(config)
  system.actorOf(BoxOffice.props, BoxOffice.name)
}
