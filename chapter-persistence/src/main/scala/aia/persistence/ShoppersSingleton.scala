package aia.persistence
//<start id="persistence-shoppers-singleton"/>

import akka.actor._
import akka.persistence._

import akka.contrib.pattern.ClusterSingletonManager
import akka.contrib.pattern.ClusterSingletonProxy

object ShoppersSingleton {
  def props = Props(new ShoppersSingleton)
  def name = "shoppers-singleton"
}

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
    case command: Shopper.Command => shoppers forward command
  }
}
//<end id="persistence-shoppers-singleton"/>

//<start id="persistence-shoppers-singleton-actor"/>
object Shoppers {
  def props = Props(new Shoppers)
  def name = "shoppers"

  sealed trait Event
  case class ShopperCreated(shopperId: Long)
}

class Shoppers extends PersistentActor
    with ShopperLookup {
  import Shoppers._

  def persistenceId = "shoppers"

  def receiveCommand = forwardToShopper

  override def createAndForward(cmd: Shopper.Command, shopperId: Long) = {
    val shopper = createShopper(shopperId)
    persistAsync(ShopperCreated(shopperId)) { _ =>
      forwardCommand(cmd)(shopper)
    }
  }

  def receiveRecover = {
    case ShopperCreated(shopperId) =>
      context.child(Shopper.name(shopperId))
        .getOrElse(createShopper(shopperId))
  }
}
//<end id="persistence-shoppers-singleton-actor"/>
