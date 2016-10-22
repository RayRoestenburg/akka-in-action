package aia.deploy


import akka.actor.{ Props, ActorSystem }
import scala.concurrent.duration._

object BootHello extends App {

  val system = ActorSystem("hellokernel")

  val actor = system.actorOf(Props[HelloWorld])
  val config = system.settings.config
  val timer = config.getInt("helloWorld.timer")
  system.actorOf(Props(
      new HelloWorldCaller(
        timer millis,
        actor)))
}
