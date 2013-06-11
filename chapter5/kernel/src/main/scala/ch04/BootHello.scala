package ch04

//<start id="ch04-boot"/>
import akka.actor.{ Props, ActorSystem }
import akka.kernel.Bootable
import scala.concurrent.duration._

class BootHello extends Bootable {          //<co id="ch04-boot-1"/>

  val system = ActorSystem("hellokernel")   //<co id="ch04-boot-2"/>

  def startup = {                           //<co id="ch04-boot-3"/>
    val actor = system.actorOf(
      Props[HelloWorld])                    //<co id="ch04-boot-4"/>
    val config = system.settings.config     //<co id="ch04-boot-5"/>
    val timer = config.getInt("helloWorld.timer")
    system.actorOf(Props(
      new HelloWorldCaller(                 //<co id="ch04-boot-6"/>
        timer millis,                       //<co id="ch04-boot-7"/>
        actor)))                            //<co id="ch04-boot-8"/>
  }

  def shutdown = {                          //<co id="ch04-boot-9"/>
    system.shutdown()
  }
}
//<end id="ch04-boot"/>