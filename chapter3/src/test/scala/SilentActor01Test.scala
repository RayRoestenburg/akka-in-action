package ch02

import org.scalatest.{ WordSpec }
import org.scalatest.matchers.MustMatchers
import akka.testkit.TestKit
import akka.actor._
//This test is ignored in the BookBuild, it's added to the defaultExcludedNames
//<start id="ch02-silentactor-test01"/>
class SilentActor01Test extends TestKit(ActorSystem("testsystem")) //<co id="ch02-silentactor-test01-provide-system"/>
  with WordSpec //<co id="ch02-silentactor-test01-wordspec"/>
  with MustMatchers //<co id="ch02-silentactor-test01-mustmatchers"/>
  with StopSystemAfterAll { //<co id="ch02-silentactor-test01-stopsystem"/>

  "A Silent Actor" must { //<co id="ch02-silentactor-test01-textmust"/>
    "change state when it receives a message, single threaded" in { //<co id="ch02-silentactor-test01-in"/>
      //Write the test, first fail
      fail("not implemented yet")
    }
    "change state when it receives a message, multi-threaded" in {
      //Write the test, first fail
      fail("not implemented yet")
    }
  }

}
//<end id="ch02-silentactor-test01"/>

//<start id="ch02-silentactor-test01-imp"/>
class SilentActor extends Actor {
  def receive = {
    case msg => //<co id="ch02-silentactor-test01-imp-unit"/>
  }
}
//<end id="ch02-silentactor-test01-imp"/>
