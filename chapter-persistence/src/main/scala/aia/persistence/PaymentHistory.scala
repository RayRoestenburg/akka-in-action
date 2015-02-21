package aia.persistence

import akka.actor._
import akka.persistence._

object PaymentHistory {
  def props(shopperId: Long) = Props(new PaymentHistory(shopperId))
  def name(shopperId: Long) = s"payment_history_${shopperId}"

  case object GetHistory

  case class History(items: List[Basket.Item] = Nil) {
    def paid(paidItems: List[Basket.Item]) = {
      History(paidItems ++ items)
    }
  }
}

class PaymentHistory(shopperId: Long) extends PersistentView
    with ActorLogging {
  import Basket._
  import PaymentHistory._

  def viewId = PaymentHistory.name(shopperId)

  def persistenceId = Wallet.name(shopperId)

  var history = History()

  def receive = {
    case Wallet.Paid(items, _) => history = history.paid(items)
    case GetHistory => sender() ! history
  }
}
