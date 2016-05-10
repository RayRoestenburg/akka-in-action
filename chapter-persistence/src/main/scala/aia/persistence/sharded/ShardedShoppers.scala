package aia.persistence.sharded

import akka.actor._
import aia.persistence._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}

object ShardedShoppers {
  def props= Props(new ShardedShoppers)
  def name = "sharded-shoppers"
}

class ShardedShoppers extends Actor {

  ClusterSharding(context.system).start(
    ShardedShopper.shardName,
    ShardedShopper.props,
    ClusterShardingSettings(context.system), // BUGBUG Captured all of below?
    ShardedShopper.extractEntityId,
    ShardedShopper.extractShardId
                                       )

//  ClusterSharding(context.system).start(  // BUGBUG goes with above!
//    typeName = ShardedShopper.shardName,
//    entryProps = Some(ShardedShopper.props),
//    idExtractor = ShardedShopper.idExtractor,
//    shardResolver = ShardedShopper.shardResolver
//  )

  def shardedShopper = {
    ClusterSharding(context.system).shardRegion(ShardedShopper.shardName)
  }

  def receive = {
    case cmd: Shopper.Command =>
      shardedShopper forward cmd
  }
}
