package aia.testdriven
import akka.testkit.{ CallingThreadDispatcher, EventFilter, TestKit }
import akka.actor.{ Props, ActorSystem }
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike

//<start id="ch02-helloworld-test"/>
import Greeter01Test._

class Greeter01Test extends TestKit(testSystem) //<co id="ch02-helloworld-use-system"/>
  with WordSpecLike
  with StopSystemAfterAll {

  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val dispatcherId = CallingThreadDispatcher.Id
      val props = Props[Greeter].withDispatcher(dispatcherId) //<co id="ch02-helloworld-dispatcher"/>
      val greeter = system.actorOf(props)
      EventFilter.info(message = "Hello World!",
        occurrences = 1).intercept { //<co id="ch02-helloworld-intercept"/>
          greeter ! Greeting("World")
        }
    }
  }
}

object Greeter01Test {
  val testSystem = { //<co id="ch02-helloworld-test-create-system"/>
    val config = ConfigFactory.parseString(
      """
         akka.loggers = [akka.testkit.TestEventListener]
      """)
    ActorSystem("testsystem", config)
  }
}
//<end id="ch02-helloworld-test"/>

