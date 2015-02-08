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
  sealed trait Event
  case class ShopperCreated(shopperId: Long)
}

class Shoppers extends PersistentActor {
  import Shoppers._

  def persistenceId = "shoppers"

  def receiveCommand = {
    case ForwardToBasket(shopperId, basketCommand) =>
      def forwardCommand(actor: ActorRef) = actor forward basketCommand
      def createAndForward() = {
        persistAsync(ShopperCreated(shopperId)) { event =>
          val ShopperCreated(shopperId) = event
          val shopper = context.actorOf(Shopper.props(shopperId),
            Shopper.name(shopperId))
          shopper forward basketCommand
        }
      }

      context.child(Shopper.name(shopperId))
        .fold(createAndForward())(forwardCommand)
  }

  def receiveRecover = {
    case ShopperCreated(shopperId) =>
      context.actorOf(Shopper.props(shopperId),
          Shopper.name(shopperId))
  }
}
