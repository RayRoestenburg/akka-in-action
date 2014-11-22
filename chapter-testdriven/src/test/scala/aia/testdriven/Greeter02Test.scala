package aia.testdriven

import akka.testkit.{ TestKit }
import org.scalatest.WordSpecLike
import akka.actor._


//<start id="ch02-helloworld-test2"/>
class Greeter02Test extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with StopSystemAfterAll {

  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val props = Props(new Greeter02(Some(testActor))) //<co id="ch02-helloworld-test2-add-listener"/>
      val greeter = system.actorOf(props, "greeter02-1")
      greeter ! Greeting("World")
      expectMsg("Hello World!") //<co id="ch02-helloworld-test2-expectMsg"/>
    }
    "say something else and see what happens" in {
      val props = Props(new Greeter02(Some(testActor)))
      val greeter = system.actorOf(props, "greeter02-2")
      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])
      greeter ! "World"
      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
    }
  }
}
//<end id="ch02-helloworld-test2"/>
//<start id="ch02-helloworld-imp2"/>
class Greeter02(listener: Option[ActorRef] = None) //<co id="ch02-helloworld-imp2-constructor"/>
  extends Actor with ActorLogging {
  def receive = {
    case Greeting(who) =>
      val message = "Hello " + who + "!"
      log.info(message)
      listener.foreach(_ ! message) //<co id="ch02-helloworld-imp2-send-to-listener"/>
  }
}
//<end id="ch02-helloworld-imp2"/>
