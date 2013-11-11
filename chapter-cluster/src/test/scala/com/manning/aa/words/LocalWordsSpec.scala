package com.manning.aa
package words

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.MustMatchers

import JobReceptionist._


class LocalWordsSpec extends TestKit(ActorSystem("test"))
                        with WordSpec
                        with MustMatchers
                        with BeforeAndAfterAll
                        with ImplicitSender {

  val receptionist = system.actorOf(JobReceptionist.props, JobReceptionist.name)

  "the words" must {
    "finish a job" in {
      receptionist ! JobRequest("test1", List("this", "is", "a", "test"))
      expectMsg(JobResponse("test1", Map("this" -> 1, "is"-> 1, "a" -> 1, "test" -> 1)))
    }
    "finish another job" in {
      receptionist ! JobRequest("test2", List("this is a test ", "this is a test", "this is", "this"))
      expectMsg(JobResponse("test2", Map("this" -> 4, "is"-> 3, "a" -> 2, "test" -> 2)))
    }

  }

  override protected def afterAll(): Unit = {
    system.shutdown()
    super.afterAll()
  }
}
