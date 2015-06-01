package aia.persistence.calculator

import akka.actor._
import akka.persistence._

import akka.testkit._
import org.scalatest._

class CalculatorHistorySpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  "The CalculatorHistory" should {
    "show the number of times every operation is used after starting" in {
      val calc = system.actorOf(Calculator.props, Calculator.name)
      calc ! Calculator.Add(1d)
      calc ! Calculator.GetResult
      expectMsg(1d)

      calc ! Calculator.Subtract(0.5d)
      calc ! Calculator.GetResult
      expectMsg(0.5d)

      calc ! Calculator.Multiply(2d)
      calc ! Calculator.GetResult
      expectMsg(1d)

      calc ! Calculator.Multiply(4d)
      calc ! Calculator.GetResult
      expectMsg(4d)

      calc ! Calculator.Divide(4d)
      calc ! Calculator.GetResult
      expectMsg(1d)

      val calcHistory = system.actorOf(CalculatorHistory.props, CalculatorHistory.name)
      calcHistory ! CalculatorHistory.GetHistory
      expectMsg(CalculatorHistory.History(added = 1, subtracted = 1, multiplied = 2, divided = 1))
      killActors(calc, calcHistory)
    }

    "show the number of times every operation is used after it is forced to update" in {
      val calc = system.actorOf(Calculator.props, Calculator.name)
      val calcHistory = system.actorOf(CalculatorHistory.props, CalculatorHistory.name+"test")

      calc ! Calculator.Add(1d)
      calc ! Calculator.GetResult
      expectMsg(2d)
      calcHistory ! Update(await = true)
      calcHistory ! CalculatorHistory.GetHistory
      expectMsg(CalculatorHistory.History(added = 2, subtracted = 1, multiplied = 2, divided = 1))
      killActors(calc, calcHistory)
    }
  }
}
