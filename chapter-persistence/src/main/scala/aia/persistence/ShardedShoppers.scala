package aia.persistence

import akka.actor._
import akka.contrib.pattern.ClusterSharding

object ShardedShoppers {
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
    case ShardedShopper.ForwardToBasket(_, basketCommand) =>
      shardedShopper forward basketCommand
  }
}
