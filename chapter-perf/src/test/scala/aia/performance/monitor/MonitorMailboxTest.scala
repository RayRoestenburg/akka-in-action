package aia.performance.monitor

import akka.testkit.TestProbe
import akka.actor.{ Props, Actor, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import concurrent.duration._
import com.typesafe.config.ConfigFactory

class MonitorMailboxTest extends WordSpecLike with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("monitor/mailbox")
  implicit val system = ActorSystem("MonitorMailboxTest", configuration)

  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  "mailbox" must {

    "send statistics with dispatcher" in {
      val statProbe = TestProbe()
      system.eventStream.subscribe(
        statProbe.ref,
        classOf[MailboxStatistics])
      val testActor = system.actorOf(Props(
        new ProcessTestActor(1.second))
        .withDispatcher("my-dispatcher"), "monitorActor")
      statProbe.send(testActor, "message")
      statProbe.send(testActor, "message2")
      statProbe.send(testActor, "message3")

      val stat = statProbe.expectMsgType[MailboxStatistics]
      println(stat)
      stat.queueSize must be(1)
      val stat2 = statProbe.expectMsgType[MailboxStatistics]
      println(stat2)
      stat2.queueSize must (be(2) or be(1))
      val stat3 = statProbe.expectMsgType[MailboxStatistics]
      println(stat3)
      stat3.queueSize must (be(3) or be(2))

      Thread.sleep(2000)
      system.stop(testActor)
      system.eventStream.unsubscribe(statProbe.ref)
    }

    "send statistics with default" in {

      val statProbe = TestProbe()
      system.eventStream.subscribe(
        statProbe.ref,
        classOf[MailboxStatistics])
      val testActor = system.actorOf(Props(
        new ProcessTestActor(1.second)), "monitorActor2")
      statProbe.send(testActor, "message")
      statProbe.send(testActor, "message2")
      statProbe.send(testActor, "message3")
      val stat = statProbe.expectMsgType[MailboxStatistics]

      stat.queueSize must be(1)
      val stat2 = statProbe.expectMsgType[MailboxStatistics]

      stat2.queueSize must (be(2) or be(1))
      val stat3 = statProbe.expectMsgType[MailboxStatistics]

      stat3.queueSize must (be(3) or be(2))


      Thread.sleep(2000)
      system.stop(testActor)
      system.eventStream.unsubscribe(statProbe.ref)
    }
  }
}

class ProcessTestActor(serviceTime: Duration) extends Actor {
  def receive = {
    case _ => {
      Thread.sleep(serviceTime.toMillis)
    }
  }
}
