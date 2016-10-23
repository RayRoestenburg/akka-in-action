package aia.testdriven


import akka.actor.{ActorLogging, Actor}

case class Greeting(message: String)

class Greeter extends Actor with ActorLogging {
  def receive = {
    case Greeting(message) => log.info("Hello {}!", message)
  }
}

