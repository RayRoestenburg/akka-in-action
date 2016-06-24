package aia.persistence.sharded

import aia.persistence._
import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

object ShardedShoppers {
  def props= Props(new ShardedShoppers)
  def name = "sharded-shoppers"
}

class ShardedShoppers extends Actor {

  ClusterSharding(context.system).start(
    ShardedShopper.shardName,
    ShardedShopper.props,
    ClusterShardingSettings(context.system),
    ShardedShopper.extractEntityId,
    ShardedShopper.extractShardId
  )

  def shardedShopper = {
    ClusterSharding(context.system).shardRegion(ShardedShopper.shardName)
  }

  def receive = {
    case cmd: Shopper.Command =>
      shardedShopper forward cmd
  }
}
