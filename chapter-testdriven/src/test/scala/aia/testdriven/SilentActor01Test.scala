package aia.testdriven

import org.scalatest.{WordSpecLike, MustMatchers}
import akka.testkit.TestKit
import akka.actor._

//This test is ignored in the BookBuild, it's added to the defaultExcludedNames

class SilentActor01Test extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {
  // Commented to make the travis build pass, this is the original test in the book
  // "A Silent Actor" must {
  //   "change state when it receives a message, single threaded" in {
  //     //Write the test, first fail
  //     fail("not implemented yet")
  //   }
  //   "change state when it receives a message, multi-threaded" in {
  //     //Write the test, first fail
  //     fail("not implemented yet")
  //   }
  // }
  "A Silent Actor" must {
    "change state when it receives a message, single threaded" ignore {
      //Write the test, first fail
      fail("not implemented yet")
    }
    "change state when it receives a message, multi-threaded" ignore {
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

