package aia.persistence
//<start id="persistence-shopper"/>

import akka.actor._

object Shopper {
  def props(shopperId: Long) = Props(new Shopper)
  def name(shopperId: Long) = shopperId.toString

  trait Command {
    def shopperId: Long //<co id="shopper_command"/>
  }

  case class PayBasket(shopperId: Long) extends Command
  // for simplicity every shopper got 40k to spend.
  val cash = 40000
}

class Shopper extends Actor {
  import Shopper._

  def shopperId = self.path.name.toLong

  val basket = context.actorOf(Basket.props,
    Basket.name(shopperId))


  val wallet = context.actorOf(Wallet.props(shopperId, cash),
    Wallet.name(shopperId))

  def receive = {
    case cmd: Basket.Command => basket forward cmd
    case cmd: Wallet.Command => wallet forward cmd

    case PayBasket(shopperId) => basket ! Basket.GetItems(shopperId)
    case Items(list) => wallet ! Wallet.Pay(list, shopperId)
    case Wallet.Paid(_, shopperId) => basket ! Basket.Clear(shopperId)
  }
}
//<end id="persistence-shopper"/>


// alternative PayBasket handling:
// issue: ask timeout
// benefit: can report back to sender of final result.
//
// case PayBasket(shopperId) =>
//   import scala.concurrent.duration._
//   import context.dispatcher
//   import akka.pattern.ask
//   implicit val timeout = akka.util.Timeout(10 seconds)
//   for {
//     items <- basket.ask(Basket.GetItems(shopperId)).mapTo[Items]
//     paid <- wallet.ask(Wallet.Pay(items.list, shopperId)).mapTo[Wallet.Paid]
//   } yield {
//     basket ! Basket.Clear(shopperId)
//   }
