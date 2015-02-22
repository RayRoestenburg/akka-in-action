package aia.persistence

import akka.actor._

object LocalShoppers {
  def props = Props(new LocalShoppers)
  def name = "local-shoppers"
}

class LocalShoppers extends Actor
    with ShopperLookup {
  def receive = forwardToShopper
}

trait ShopperLookup {
  implicit def context: ActorContext

  def forwardToShopper: Actor.Receive = {
    case cmd: Shopper.Command =>
      context.child(Shopper.name(cmd.shopperId))
        .fold(createAndForward(cmd, cmd.shopperId))(forwardCommand(cmd))
  }

  def forwardCommand(cmd: Shopper.Command)(shopper: ActorRef) =
    shopper forward cmd

  def createAndForward(cmd: Shopper.Command, shopperId: Long) = {
    createShopper(shopperId) forward cmd
  }

  def createShopper(shopperId: Long) =
    context.actorOf(Shopper.props(shopperId),
      Shopper.name(shopperId))
}
