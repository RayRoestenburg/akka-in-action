package ch02

import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ Props, Actor, ActorSystem }
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import akka.util.Timeout
import scala.concurrent.Await
import scala.util.{ Try, Success, Failure }

//<start id="ch02-echoactor-test-start"/>
class EchoActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpec
  with MustMatchers
  with ImplicitSender //<co id="implicit-sender"/>
  with StopSystemAfterAll {
//<end id="ch02-echoactor-test-start"/>

  "An EchoActor" must {
    "Reply with the same message it receives" in {
      //<start id="ch02-echoactor-test"/>
      import akka.pattern.ask //<co id="ch02-echoactor-test-import-ask"/>
      import scala.concurrent.duration._ //<co id="ch02-echoactor-test-duration"/>
      implicit val timeout = Timeout(3 seconds) //<co id="ch02-echoactor-test-timeout"/>
      implicit val ec = system.dispatcher //<co id="ch02-echoactor-test-execution-context"/>
      val echo = system.actorOf(Props[EchoActor], "echo1")
      val future = echo.ask("some message") //<co id="ch02-echoactor-test-future"/>
      future.onComplete { //<co id="ch02-echoactor-test-oncomplete"/>
        case Failure(_)   => //handle failure  //<co id="ch02-echoactor-test-left"/>
        case Success(msg) => //handle success //<co id="ch02-echoactor-test-right"/>
      }
      //<end id="ch02-echoactor-test"/>
      Await.ready(future, timeout.duration)
    }
    //<start id="ch02-echoactor-test-oneways"/>
    "Reply with the same message it receives without ask" in {
      val echo = system.actorOf(Props[EchoActor], "echo2")
      echo ! "some message" //<co id="ch02-echoactor-test-tell-with-implicit-sender"/>
      expectMsg("some message") //<co id="ch02-echoactor-test-expectMsg"/>

    }
    //<end id="ch02-echoactor-test-oneways"/>
  }
}
//<start id="ch02-echoactor-imp"/>

class EchoActor extends Actor {
  def receive = {
    case msg =>
      sender ! msg
  }
}
//<end id="ch02-echoactor-imp"/>
