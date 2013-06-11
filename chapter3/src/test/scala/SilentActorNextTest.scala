package ch02

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import akka.testkit.{ TestActorRef, CallingThreadDispatcher, TestKit }
import akka.actor._

package silentactor02 {
  class SilentActorTest extends TestKit(ActorSystem("testsystem"))
    with WordSpec
    with MustMatchers
    with StopSystemAfterAll {

    "A Silent Actor" must {
      //<start id="ch02-silentactor-test02"/>
      "change internal state when it receives a message, single" in {
        import SilentActorProtocol._ //<co id="ch02-silentactor-test02-import-protocol"/>

        val silentActor = TestActorRef[SilentActor] //<co id="ch02-silentactor-test02-TestActorRef"/>
        silentActor ! SilentMessage("whisper")
        silentActor.underlyingActor.state must (contain("whisper")) //<co id="ch02-silentactor-test02-assert-state"/>
      }
      //<end id="ch02-silentactor-test02"/>
    }
  }

  //<start id="ch02-silentactor-test02-imp"/>
  object SilentActorProtocol { //<co id="ch02-silentactor-test02-protocol"/>
    case class SilentMessage(data: String) //<co id="ch02-silentactor-test02-message"/>
    case class GetState(receiver: ActorRef)
  }

  class SilentActor extends Actor {
    import SilentActorProtocol._
    var internalState = Vector[String]()

    def receive = {
      case SilentMessage(data) =>
        internalState = internalState :+ data //<co id="ch02-silentactor-test02-internal-state"/>
    }

    def state = internalState //<co id="ch02-silentactor-test02-internal-state-method"/>
  }
}
//<end id="ch02-silentactor-test02-imp"/>
package silentactor03 {

  class SilentActorTest extends TestKit(ActorSystem("testsystem"))
    with WordSpec
    with MustMatchers
    with StopSystemAfterAll {

    "A Silent Actor" must {
      //<start id="ch02-silentactor-test03"/>
      "change internal state when it receives a message, multi" in {
        import SilentActorProtocol._ //<co id="ch02-silentactor-test03-import-protocol"/>

        val silentActor = system.actorOf(Props[SilentActor], "s3") //<co id="ch02-silentactor-test03-create-actor"/>
        silentActor ! SilentMessage("whisper1")
        silentActor ! SilentMessage("whisper2")
        silentActor ! GetState(testActor) //<co id="ch02-silentactor-test03-get-state"/>
        expectMsg(Vector("whisper1", "whisper2")) //<co id="ch02-silentactor-test03-expectMsg"/>
      }
      //<end id="ch02-silentactor-test03"/>
    }

  }

  //<start id="ch02-silentactor-test03-imp"/>

  object SilentActorProtocol {
    case class SilentMessage(data: String)
    case class GetState(receiver: ActorRef) //<co id="ch02-silentactor-test03-getstate-msg"/>
  }

  class SilentActor extends Actor {
    import SilentActorProtocol._
    var internalState = Vector[String]()

    def receive = {
      case SilentMessage(data) =>
        internalState = internalState :+ data
      case GetState(receiver) => receiver ! internalState //<co id="ch02-silentactor-test03-process-getstate-msg"/>
    }
  }
  //<end id="ch02-silentactor-test03-imp"/>
}