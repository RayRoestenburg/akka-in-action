package aia.persistence.sharded

import akka.actor._
import akka.contrib.pattern.ClusterSharding

import aia.persistence._

object ShardedShoppers {
  def props= Props(new ShardedShoppers)
  def name = "sharded-shoppers"
}

class ShardedShoppers extends Actor {
  ClusterSharding(context.system).start(
    typeName = ShardedShopper.shardName,
    entryProps = Some(ShardedShopper.props),
    idExtractor = ShardedShopper.idExtractor,
    shardResolver = ShardedShopper.shardResolver
  )

  def shardedShopper = ClusterSharding(context.system).shardRegion(ShardedShopper.shardName)

  def receive = {
    case cmd: Shopper.Command =>
      shardedShopper forward cmd
  }
}
