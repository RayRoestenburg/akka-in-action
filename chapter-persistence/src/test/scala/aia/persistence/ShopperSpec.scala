package aia.persistence

import akka.actor._
import akka.testkit._
import org.scalatest._

class ShopperSpec extends PersistenceSpec(ActorSystem("test"))
    with WordSpecLike
    with PersistenceCleanup {

  "The shopper" should {
    "be able to put items in the shopping basket and view the basket" in {
      val shopperId = 1L
      val shopperName = s"shopper-$shopperId"
      val shopper = system.actorOf(Shopper.props(shopperId), shopperName)
      val macbookPro = Basket.Item("Apple Macbook Pro 15 inch", 1)
      val macPro = Basket.Item("Apple Mac Pro", 1)
      val displays = Basket.Item("4K Display", 3)
      shopper ! Basket.Add(macbookPro)
      shopper ! Basket.Add(displays)
      shopper ! Basket.Add(macPro)
      shopper ! Basket.Remove(macbookPro)
      shopper ! Basket.GetItems
      expectMsg(Basket.Items(List(displays, macPro)))
      killActors(shopper)

      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems
      expectMsg(Basket.Items(List(displays, macPro)))
    }
  }
}
