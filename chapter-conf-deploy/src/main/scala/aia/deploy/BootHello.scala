package aia.deploy

//<start id="ch07-boot"/>
import akka.actor.{ Props, ActorSystem }
import scala.concurrent.duration._

object BootHello extends App {          //<co id="ch07-boot-1"/>

  val system = ActorSystem("hellokernel")   //<co id="ch07-boot-2"/>

  val actor = system.actorOf(Props[HelloWorld])                    //<co id="ch07-boot-4"/>
  val config = system.settings.config     //<co id="ch07-boot-5"/>
  val timer = config.getInt("helloWorld.timer")
  system.actorOf(Props(
      new HelloWorldCaller(                 //<co id="ch07-boot-6"/>
        timer millis,                       //<co id="ch07-boot-7"/>
        actor)))                            //<co id="ch07-boot-8"/>
}
//<end id="ch07-boot"/>