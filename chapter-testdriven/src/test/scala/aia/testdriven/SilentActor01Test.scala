package aia.testdriven

import org.scalatest.{WordSpecLike, MustMatchers}
import akka.testkit.TestKit
import akka.actor._

//This test is ignored in the BookBuild, it's added to the defaultExcludedNames

class SilentActor01Test extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {

  "A Silent Actor" must {
    "change state when it receives a message, single threaded" in {
      //Write the test, first fail
      fail("not implemented yet")
    }
    "change state when it receives a message, multi-threaded" in {
      //Write the test, first fail
      fail("not implemented yet")
    }
  }

}



class SilentActor extends Actor {
  def receive = {
    case msg =>
  }
}

