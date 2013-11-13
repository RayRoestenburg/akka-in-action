package com.manning.aa.words

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

object JobWorkerMain extends App {
  val config = ConfigFactory.load("worker")
  val system = ActorSystem("words", config)
}
