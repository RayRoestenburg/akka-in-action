package aia.persistence

import akka.actor._
import akka.persistence._

object Wallet {
  def props(shopperId: Long) = Props(new Wallet(shopperId))
  def name(shopperId: Long) = s"wallet_${shopperId}"

  sealed trait Command
  case class Pay(items: Basket.Items) extends Command

  sealed trait Event
  case class Paid(items: Basket.Items) extends Event
}

class Wallet(shopperId: Long) extends PersistentActor
    with ActorLogging {
      import Wallet._

  var amountSpent: BigDecimal = 0

  def persistenceId = s"${self.path.name}"

  def receiveCommand = {
    case Pay(items) => persist(Paid(items))(updateState)
  }

  def receiveRecover = {
    case event: Event => updateState(event)
  }

  private val updateState: (Event ⇒ Unit) = {
    case paidItems @ Paid(items) =>
      amountSpent = amountSpent + items.map(_.price).foldLeft(BigDecimal(0)){ _ + _ }
      context.system.eventStream.publish(paidItems)
  }
}
