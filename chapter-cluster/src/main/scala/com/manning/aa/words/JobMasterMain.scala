package com.manning.aa.words

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object JobMasterMain extends App {

  val config = ConfigFactory.load("master")
  val system = ActorSystem("words", config)

  system.actorOf(Props[JobReceptionist], "receptionist")
}
