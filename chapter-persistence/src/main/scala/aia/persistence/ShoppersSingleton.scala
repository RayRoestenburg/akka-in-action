package aia.persistence
//<start id="persistence-shoppers-singleton"/>

import akka.actor._
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import akka.persistence._

object ShoppersSingleton {
  def props = Props(new ShoppersSingleton)
  def name = "shoppers-singleton"
}

class ShoppersSingleton extends Actor {

  val singletonManager = context.system.actorOf(
    ClusterSingletonManager.props(
      Shoppers.props,
      PoisonPill,
      ClusterSingletonManagerSettings(context.system)
        .withRole(None)
        .withSingletonName(Shoppers.name)
    )
  )

  val shoppers = context.system.actorOf(
    ClusterSingletonProxy.props(
      singletonManager.path.child(Shoppers.name)
        .toStringWithoutAddress,
      ClusterSingletonProxySettings(context.system)
        .withRole(None)
        .withSingletonName("shoppers-proxy")
    )
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

  override def createAndForward(
    cmd: Shopper.Command, 
    shopperId: Long
  ) = {
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
