package aia.persistence

import akka.actor._

object LocalShoppers {
  def props = Props(new LocalShoppers)
  def name = "local-shoppers"
}

class LocalShoppers extends Actor {
  def receive = {
    case cmd: Shopper.Command =>
      val shopperId = cmd.shopperId
      val shopper = context.child(Shopper.name(shopperId))
        .getOrElse(context.actorOf(Shopper.props(shopperId),
          Shopper.name(shopperId)))
      shopper forward cmd
  }
}
