package aia.channels

import akka.testkit.{ ImplicitSender, TestProbe, TestKit }
import akka.actor.{ PoisonPill, Props, DeadLetter, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import java.util.Date

class DeadLetterTest extends TestKit(ActorSystem("DeadLetterTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers
  with ImplicitSender {

  override def afterAll()  {
    system.terminate()
  }

  "DeadLetter" must {
    "catch messages send to deadLetters" in {
      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe(
        deadLetterMonitor.ref,
        classOf[DeadLetter])

      val msg = new StateEvent(new Date(), "Connected")
      system.deadLetters ! msg

      val dead = deadLetterMonitor.expectMsgType[DeadLetter]
      dead.message must be(msg)
      dead.sender must be(testActor)
      dead.recipient must be(system.deadLetters)
    }
    "catch deadLetter messages send to deadLetters" in {
      //<start id="ch09-deadLetter-send-test"/>
      val deadLetterMonitor = TestProbe()
      val actor = system.actorOf(Props[EchoActor], "echo") //<co id="ch09-deadLetter-send-test-1"/>

      system.eventStream.subscribe(
        deadLetterMonitor.ref,
        classOf[DeadLetter])

      val msg = new Order("me", "Akka in Action", 1)
      val dead = DeadLetter(msg, testActor, actor) //<co id="ch09-deadLetter-send-test-2"/>
      system.deadLetters ! dead

      deadLetterMonitor.expectMsg(dead) //<co id="ch09-deadLetter-send-test-3"/>

      system.stop(actor)
      //<end id="ch09-deadLetter-send-test"/>
    }

    "catch messages send to terminated Actor" in {
      //<start id="ch09-deadLetter-test"/>
      val deadLetterMonitor = TestProbe()

      system.eventStream.subscribe( //<co id="ch09-deadLetter-test-0"/>
        deadLetterMonitor.ref,
        classOf[DeadLetter])

      val actor = system.actorOf(Props[EchoActor], "echo")
      actor ! PoisonPill //<co id="ch09-deadLetter-test-1"/>
      val msg = new Order("me", "Akka in Action", 1)
      actor ! msg //<co id="ch09-deadLetter-test-2"/>

      val dead = deadLetterMonitor.expectMsgType[DeadLetter] //<co id="ch09-deadLetter-test-3"/>
      dead.message must be(msg)
      dead.sender must be(testActor)
      dead.recipient must be(actor)
      //<end id="ch09-deadLetter-test"/>
    }

  }
}
