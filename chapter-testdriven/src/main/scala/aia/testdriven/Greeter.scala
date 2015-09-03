package aia.testdriven

//<start id="testdriven-greeter"/>
import akka.actor.{ActorLogging, Actor}

case class Greeting(message: String)

class Greeter extends Actor with ActorLogging {
  def receive = {
    case Greeting(message) => log.info("Hello {}!", message) //<co id="say_hello_something"/>
  }
}
//<end id="testdriven-greeter"/>
