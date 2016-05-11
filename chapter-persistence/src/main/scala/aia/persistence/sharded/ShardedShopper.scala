package aia.persistence.sharded

import aia.persistence._
import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate

object ShardedShopper {
  def props = Props(new ShardedShopper)
  def name(shopperId: Long) = shopperId.toString


  case object StopShopping

  val shardName: String = "shoppers"

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Shopper.Command => (cmd.shopperId.toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Shopper.Command => (cmd.shopperId % 12).toString
  }
}

class ShardedShopper extends Shopper {
  import ShardedShopper._

  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  override def unhandled(msg: Any) = msg match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = ShardedShopper.StopShopping)
    case StopShopping => context.stop(self)
  }
}
