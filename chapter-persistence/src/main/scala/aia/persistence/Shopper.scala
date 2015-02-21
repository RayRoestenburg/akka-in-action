package aia.persistence

import akka.actor._

object Shopper {
  def props(shopperId: Long) = Props(new Shopper)
  def name(shopperId: Long) = shopperId.toString

  trait Command {
    def shopperId: Long
  }

  case class PayBasket(shopperId: Long) extends Command
}

class Shopper extends Actor {
  import Shopper._

  def shopperId = self.path.name.toLong

  val basket = context.actorOf(Basket.props,
    Basket.name(shopperId))

  val wallet = context.actorOf(Wallet.props(shopperId),
    Wallet.name(shopperId))

  def receive = {
    case cmd: Basket.Command => basket forward cmd
    case cmd: Wallet.Command => wallet forward cmd
    case PayBasket(shopperId) => basket ! Basket.GetItems(shopperId)
    case Basket.Items(list) => wallet ! Wallet.Pay(list, shopperId)
    case Wallet.Paid(_, shopperId) => basket ! Basket.Clear(shopperId)
  }
}
