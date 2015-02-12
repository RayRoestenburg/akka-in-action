package aia.persistence

import akka.actor._
import akka.persistence._

object Wallet {
  def props(shopperId: Long) = Props(new Wallet(shopperId))
  def name(shopperId: Long) = s"wallet_${shopperId}"

  sealed trait Command
  case class Pay(items: Basket.Items) extends Command
  case object CheckPocket extends Command
  case object SpentHowMuch extends Command

  case class AmountSpent(amount: BigDecimal)
  case class NotEnoughCash(left: BigDecimal)
  case class Cash(left: BigDecimal)

  sealed trait Event
  case class Paid(items: Basket.Items) extends Event
}

class Wallet(shopperId: Long) extends PersistentActor
    with ActorLogging {
      import Wallet._
  var cash: BigDecimal = 40000
  var amountSpent: BigDecimal = 0

  def persistenceId = s"${self.path.name}"

  def receiveCommand = {
    case Pay(items) =>
      val totalSpent = addSpending(items)
      if(cash - totalSpent > 0) {
        persist(Paid(items)) { paidItems =>
          updateState(paidItems)
          sender() ! paidItems
          context.system.eventStream.publish(paidItems)
        }
      } else context.system.eventStream.publish(NotEnoughCash(cash - amountSpent))
    case CheckPocket => sender() ! Cash(cash - amountSpent)
    case SpentHowMuch => sender() ! AmountSpent(amountSpent)
  }

  def receiveRecover = {
    case event: Event => updateState(event)
  }

  private val updateState: (Event ⇒ Unit) = {
    case paidItems @ Paid(items) =>
      amountSpent = addSpending(items)
  }

  private def addSpending(items: Basket.Items) =
    amountSpent + items.foldLeft(BigDecimal(0)){ (acc, item) =>
      acc + (item.price * item.number)
    }
}
