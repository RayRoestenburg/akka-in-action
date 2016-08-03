//<start id="persistence-calc"/>
package aia.persistence.calculator

import akka.actor._
import akka.persistence._

object Calculator {
  def props = Props(new Calculator)
  def name = "my-calculator"

  //<start id="persistence-calc_commands_events"/>
  sealed trait Command //<co id="persistence-calc_command"/>
  case object Clear extends Command
  case class Add(value: Double) extends Command
  case class Subtract(value: Double) extends Command
  case class Divide(value: Double) extends Command
  case class Multiply(value: Double) extends Command
  case object PrintResult extends Command
  case object GetResult extends Command

  sealed trait Event //<co id="persistence-calc_event"/>
  case object Reset extends Event
  case class Added(value: Double) extends Event
  case class Subtracted(value: Double) extends Event
  case class Divided(value: Double) extends Event
  case class Multiplied(value: Double) extends Event
  //<end id="persistence-calc_commands_events"/>

  //<start id="persistence-calc_result"/>
  case class CalculationResult(result: Double = 0) {
    def reset = copy(result = 0)
    def add(value: Double) = copy(result = this.result + value)
    def subtract(value: Double) = copy(result = this.result - value)
    def divide(value: Double) = copy(result = this.result / value)
    def multiply(value: Double) = copy(result = this.result * value)
  }
  //<end id="persistence-calc_result"/>
}

//<start id="persistence-extend_persistent_actor"/>
class Calculator extends PersistentActor with ActorLogging {
  import Calculator._

  def persistenceId = Calculator.name

  var state = CalculationResult()
  // more code to follow ..
  //<end id="persistence-extend_persistent_actor"/>


  //<start id="persistence-receive_recover_calc"/>
  val receiveRecover: Receive = {
    case event: Event => updateState(event)
    case RecoveryCompleted => log.info("Calculator recovery completed") //<co id="recovery_completed"/>
  }
  //<end id="persistence-receive_recover_calc"/>

  //<start id="persistence-receive_command_calc"/>
  val receiveCommand: Receive = {
    case Add(value)      => persist(Added(value))(updateState)
    case Subtract(value) => persist(Subtracted(value))(updateState)
    case Divide(value)   => if(value != 0) persist(Divided(value))(updateState)
    case Multiply(value) => persist(Multiplied(value))(updateState)
    case PrintResult     => println(s"the result is: ${state.result}")
    case GetResult       => sender() ! state.result
    case Clear           => persist(Reset)(updateState)
  }
  //<end id="persistence-receive_command_calc"/>

  //<start id="persistence-update_state_calc"/>
  val updateState: Event => Unit = {
    case Reset             => state = state.reset
    case Added(value)      => state = state.add(value)
    case Subtracted(value) => state = state.subtract(value)
    case Divided(value)    => state = state.divide(value)
    case Multiplied(value) => state = state.multiply(value)
  }
  //<end id="persistence-update_state_calc"/>
}
//<end id="persistence-calc"/>
