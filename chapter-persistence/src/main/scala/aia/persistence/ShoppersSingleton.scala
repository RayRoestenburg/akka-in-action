package aia.persistence

import akka.actor._
import akka.persistence._

import akka.contrib.pattern.ClusterSingletonManager
import akka.contrib.pattern.ClusterSingletonProxy


class ShoppersSingleton extends Actor {
  val singletonManager = context.system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Shoppers.props,
      singletonName = Shoppers.name,
      terminationMessage = PoisonPill,
      role = None
    ), name = Shoppers.name
  )

  val shoppers = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonPath = singletonManager.path
        .child(Shoppers.name)
        .toStringWithoutAddress,
      role = None
    ), name = "shoppers-proxy"
  )

  def receive = {
    case command: Shoppers.Command =>
      shoppers forward command
  }
}

object Shoppers {
  def props = Props(new Shoppers)
  def name = "shoppers"

  sealed trait Command {
    def shopperId: Long
  }
  case class ForwardToBasket(shopperId: Long,
    basketCommand: Basket.Command) extends Command
  case class ForwardToWallet(shopperId: Long,
    walletCommand: Wallet.Command) extends Command

  sealed trait Event
  case class ShopperCreated(shopperId: Long)
}

class Shoppers extends PersistentActor {
  import Shoppers._

  def persistenceId = "shoppers"


  def receiveCommand = {
    case ForwardToBasket(shopperId, basketCommand) =>
      context.child(Shopper.name(shopperId))
        .fold(createAndForward(basketCommand, shopperId))(forwardCommand(basketCommand))
    case ForwardToWallet(shopperId, walletCommand) =>
      context.child(Shopper.name(shopperId))
        .fold(createAndForward(walletCommand, shopperId))(forwardCommand(walletCommand))
  }

  def forwardCommand(cmd: AnyRef)(actor: ActorRef) = actor forward cmd

  def createAndForward(cmd: AnyRef, shopperId: Long) = {
    val shopper = context.actorOf(Shopper.props(shopperId),
      Shopper.name(shopperId))
    persistAsync(ShopperCreated(shopperId)) { event =>
      val ShopperCreated(shopperId) = event
      shopper forward cmd
    }
  }

  def receiveRecover = {
    case ShopperCreated(shopperId) =>
      context.actorOf(Shopper.props(shopperId),
          Shopper.name(shopperId))
  }
}
