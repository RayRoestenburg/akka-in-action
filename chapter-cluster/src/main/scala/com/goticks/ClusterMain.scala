package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}

object ClusterMain extends App {

  if(args.size == 1) {
    System.setProperty("NETTY_PORT",args(0))
  }

  val config = ConfigFactory.load("cluster")

  val system = ActorSystem("goticks", config)
  system.actorOf(Props[BoxOffice], "boxOffice")

}
