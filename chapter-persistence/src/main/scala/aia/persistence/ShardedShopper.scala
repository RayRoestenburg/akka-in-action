package aia.persistence

import scala.concurrent.duration._

import akka.actor._

import akka.contrib.pattern.ShardRegion
import akka.contrib.pattern.ShardRegion.Passivate

object ShardedShopper {
  def props = Props(new ShardedShopper)
  def name(shopperId: Long) = shopperId.toString

  sealed trait Command {
    def shopperId: Long
  }

  case class ForwardToBasket(shopperId: Long,
    basketCommand: Basket.Command) extends Command

  case class ForwardToWallet(shopperId: Long,
    walletCommand: Wallet.Command) extends Command

  case object Stop

  val shardName: String = "shoppers"

  val idExtractor: ShardRegion.IdExtractor = {
    case cmd: Command ⇒ (cmd.shopperId.toString, cmd)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case cmd: Command ⇒ (cmd.shopperId % 12).toString
  }
}

class ShardedShopper extends Actor {
  import ShardedShopper._

  context.setReceiveTimeout(Settings(context.system).passivateTimeout)

  def shopperId = self.path.name.toLong

  val basket = context.actorOf(Basket.props(shopperId),
    Basket.name(shopperId))

  def receive = {
    case cmd: Basket.Command => basket forward cmd
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = ShardedShopper.Stop)
    case ShardedShopper.Stop => context.stop(self)
  }
}
