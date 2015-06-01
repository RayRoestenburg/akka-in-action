package aia.persistence.calculator

import akka.actor._
import akka.persistence._

object CalculatorHistory {
  def props = Props(new CalculatorHistory)
  def name = "calculator-history"
  case object GetHistory
  case class History(added: Int = 0, subtracted: Int = 0, divided: Int = 0, multiplied: Int = 0) {
    def incrementAdded = copy(added = added + 1)
    def incrementSubtracted= copy(subtracted = subtracted + 1)
    def incrementDivided = copy(divided = divided + 1)
    def incrementMultiplied = copy(multiplied = multiplied + 1)
  }
}

class CalculatorHistory extends PersistentView {
  import Calculator._
  import CalculatorHistory._

  def viewId = CalculatorHistory.name

  def persistenceId = Calculator.name

  var history = History()

  def receive = {
    case event: Added => history = history.incrementAdded
    case event: Subtracted => history = history.incrementSubtracted
    case event: Divided => history = history.incrementDivided
    case event: Multiplied => history = history.incrementMultiplied
    case GetHistory => sender() ! history
  }
}
