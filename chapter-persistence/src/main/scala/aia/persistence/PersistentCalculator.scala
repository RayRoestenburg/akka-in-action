package aia.persistence

import akka.actor._
import akka.persistence._

object PersistentCalculator {
  def props = Props(new PersistentCalculator)
  def name = "calc"

  sealed trait Command
  case class Add(value: Double) extends Command
  case class Subtract(value: Double) extends Command
  case class Divide(value: Double) extends Command
  case class Multiply(value: Double) extends Command
  case object PrintResult extends Command

  sealed trait Event
  case class Added(value: Double) extends Event
  case class Subtracted(value: Double) extends Event
  case class Divided(value: Double) extends Event
  case class Multiplied(value: Double) extends Event

  case class CalculationResult(result: Double = 0) {
    def add(value: Double) = copy(result = this.result + value)
    def subtract(value: Double) = copy(result = this.result - value)
    def divide(value: Double) = copy(result = this.result / value)
    def multiply(value: Double) = copy(result = this.result * value)
  }
}

class PersistentCalculator extends PersistentActor {
  import PersistentCalculator._

  var state = CalculationResult()

  def persistenceId = "my-calculator"

  val receiveRecover: Receive = {
    case event: Event => updateState(event)
  }

  val receiveCommand: Receive = {
    case Add(value)      => persist(Added(value))(updateState)
    case Subtract(value) => persist(Subtracted(value))(updateState)
    case Divide(value)   => if(value != 0) persist(Divided(value))(updateState)
    case Multiply(value) => persist(Multiplied(value))(updateState)
    case PrintResult     => println(s"the result is: ${state.result}")
  }

  val updateState: Event => Unit = {
    case Added(value) => state = state.add(value)
    case Subtracted(value) => state = state.subtract(value)
    case Divided(value) => state = state.divide(value)
    case Multiplied(value) => state = state.multiply(value)
  }
}
