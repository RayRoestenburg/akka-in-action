package aia.persistence.sharded

import scala.concurrent.duration._

import akka.actor._

import akka.contrib.pattern.ShardRegion
import akka.contrib.pattern.ShardRegion.Passivate

import aia.persistence._

object ShardedShopper {
  def props = Props(new ShardedShopper)
  def name(shopperId: Long) = shopperId.toString


  case object StopShopping

  val shardName: String = "shoppers"

  val idExtractor: ShardRegion.IdExtractor = {
    case cmd: Shopper.Command => (cmd.shopperId.toString, cmd)
  }

  val shardResolver: ShardRegion.ShardResolver = {
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
