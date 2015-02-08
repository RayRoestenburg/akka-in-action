package aia.persistence

import akka.actor._

object Shopper {
  def props(shopperId: Long) = Props(new Shopper(shopperId))
  def name(shopperId: Long) = shopperId.toString
}

class Shopper(shopperId: Long) extends Actor {
  val basket = context.actorOf(Basket.props(shopperId),
    Basket.name(shopperId))

  def receive = {
    case cmd: Basket.Command => basket forward cmd
  }
}
