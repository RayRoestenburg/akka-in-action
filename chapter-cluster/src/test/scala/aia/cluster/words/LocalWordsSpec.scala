package aia.cluster
package words

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor._

import org.scalatest._
import org.scalatest.MustMatchers

import JobReceptionist._
import akka.routing.BroadcastPool


trait CreateLocalWorkerRouter extends CreateWorkerRouter { this: Actor =>
  override def createWorkerRouter: ActorRef = {
    context.actorOf(BroadcastPool(5).props(Props[JobWorker]), "worker-router")
  }
}

class TestJobMaster extends JobMaster
                       with CreateLocalWorkerRouter

class TestReceptionist extends JobReceptionist
                          with CreateMaster {
  override def createMaster(name: String): ActorRef = context.actorOf(Props[TestJobMaster], name)
}

class LocalWordsSpec extends TestKit(ActorSystem("test"))
                        with WordSpecLike
                        with MustMatchers
                        with StopSystemAfterAll
                        with ImplicitSender {
  val receptionist = system.actorOf(Props[TestReceptionist], JobReceptionist.name)

  "The words system" must {
    "count the occurrence of words in a text" in {
      receptionist ! JobRequest("test2", List("this is a test ", "this is a test", "this is", "this"))
      expectMsg(JobSuccess("test2", Map("this" -> 4, "is"-> 3, "a" -> 2, "test" -> 2)))
      expectNoMsg
    }

    "count many occurences of words in a text" in {
      val words = List("this is a test ", "this is a test", "this is", "this")
      receptionist ! JobRequest("test3", (1 to 100).map(i=> words ++ words).flatten.toList)
      expectMsg(JobSuccess("test3", Map("this" -> 800, "is"-> 600, "a" -> 400, "test" -> 400)))
      expectNoMsg
    }

    "continue to process a job with intermittent failures" in {
      // the failure is simulated by a job worker throwing an exception on finding the word FAIL in the text.
      receptionist ! JobRequest("test4", List("this", "is", "a", "test", "FAIL!"))
      expectMsg(JobSuccess("test4", Map("this" -> 1, "is"-> 1, "a" -> 1, "test" -> 1)))
      expectNoMsg
    }
  }
}
