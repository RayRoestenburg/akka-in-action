package aia.testdriven

import org.scalatest.WordSpecLike
import org.scalatest.MustMatchers
import akka.testkit.{ TestActorRef, TestKit }
import akka.actor._

package silentactor02 {

class SilentActorTest extends TestKit(ActorSystem("testsystem"))
    with WordSpecLike
    with MustMatchers
    with StopSystemAfterAll {

    "A Silent Actor" must {

      "change internal state when it receives a message, single" in {
        import SilentActor._

        val silentActor = TestActorRef[SilentActor]
        silentActor ! SilentMessage("whisper")
        silentActor.underlyingActor.state must (contain("whisper"))
      }

    }
  }


  object SilentActor {
    case class SilentMessage(data: String)
    case class GetState(receiver: ActorRef)
  }

  class SilentActor extends Actor {
    import SilentActor._
    var internalState = Vector[String]()

    def receive = {
      case SilentMessage(data) =>
        internalState = internalState :+ data
    }

    def state = internalState
  }
}

package silentactor03 {

  class SilentActorTest extends TestKit(ActorSystem("testsystem"))
    with WordSpecLike
    with MustMatchers
    with StopSystemAfterAll {

    "A Silent Actor" must {

      "change internal state when it receives a message, multi" in {
        import SilentActor._

        val silentActor = system.actorOf(Props[SilentActor], "s3")
        silentActor ! SilentMessage("whisper1")
        silentActor ! SilentMessage("whisper2")
        silentActor ! GetState(testActor)
        expectMsg(Vector("whisper1", "whisper2"))
      }

    }

  }



  object SilentActor {
    case class SilentMessage(data: String)
    case class GetState(receiver: ActorRef)
  }

  class SilentActor extends Actor {
    import SilentActor._
    var internalState = Vector[String]()

    def receive = {
      case SilentMessage(data) =>
        internalState = internalState :+ data
      case GetState(receiver) => receiver ! internalState
    }
  }

}
