package aia.persistence

import akka.actor._
import akka.testkit._
import org.scalatest._

import PersistenceSpec._

class PersistentCalculatorSpec extends PersistenceSpec(ActorSystem("test"))
    with WordSpecLike
    with PersistenceCleanup {

  "The Calculator" should {
    "Recover last known result after crash" in {
      val calc = system.actorOf(PersistentCalculator.props, PersistentCalculator.name)
      calc ! PersistentCalculator.Add(1d)
      calc ! PersistentCalculator.GetResult
      expectMsg(1d)
      calc ! PersistentCalculator.Subtract(0.5d)
      calc ! PersistentCalculator.GetResult
      expectMsg(0.5d)

      watch(calc)
      system.stop(calc)
      expectTerminated(calc)

      val calcResurrected = system.actorOf(PersistentCalculator.props, PersistentCalculator.name)
      calcResurrected ! PersistentCalculator.Add(1d)
      calcResurrected ! PersistentCalculator.GetResult
      expectMsg(1.5d)
    }
  }
}
