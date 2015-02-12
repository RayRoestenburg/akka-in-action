package aia.persistence

import akka.actor._

object Shopper {
  def props(shopperId: Long) = Props(new Shopper(shopperId))
  def name(shopperId: Long) = shopperId.toString
  sealed trait Command
  case object PayBasket
}

class Shopper(shopperId: Long) extends Actor {
  import Shopper._

  val basket = context.actorOf(Basket.props(shopperId),
    Basket.name(shopperId))

  val wallet = context.actorOf(Wallet.props(shopperId),
    Wallet.name(shopperId))

  def receive = {
    case cmd: Basket.Command => basket forward cmd
    case PayBasket =>
      basket ! Basket.GetItems
    case items: Basket.Items =>
      basket ! Basket.Clear
      wallet ! Wallet.Pay(items)
  }
}
