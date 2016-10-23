package aia.persistence

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._
import org.scalatest._

class ShopperSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val shopperId = 21L
  val shopperName = s"$shopperId"
  val macbookPro = Item("Apple Macbook Pro", 1, BigDecimal(2499.99))
  val macPro = Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Item("Apple Keyboard", 1, BigDecimal(79.99))

  val expectedTotalSpend = Wallet.AmountSpent(
    (macPro.unitPrice * macPro.number) +
    (displays.unitPrice * displays.number) +
    (appleMouse.unitPrice * appleMouse.number) +
    (appleKeyboard.unitPrice * appleKeyboard.number)
  )

  val dWave = Item("D-Wave One", 1, BigDecimal(14999999.99))

  "The shopper" should {
    "put items in the shopping basket and view the basket" in {
      val shopper = system.actorOf(Shopper.props(shopperId), shopperName)
      shopper ! Basket.Add(macbookPro, shopperId)
      shopper ! Basket.Add(displays, shopperId)
      shopper ! Basket.Add(macPro, shopperId)
      shopper ! Basket.RemoveItem(macbookPro.productId, shopperId)
      expectMsg(Some(Basket.ItemRemoved(macbookPro.productId)))
      shopper ! Basket.GetItems(shopperId)
      expectMsg(Items(displays, macPro))
      killActors(shopper)

      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems(shopperId)
      expectMsg(Items(displays, macPro))

      killActors(shopperResurrected)
    }

    "pay for items in the shopping basket" in {
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[Wallet.Paid])

      val shopper = system.actorOf(Shopper.props(shopperId), shopperName)
      shopper ! Basket.Add(appleMouse,shopperId)
      shopper ! Basket.GetItems(shopperId)
      expectMsg(Items(displays, macPro, appleMouse))
      shopper ! Shopper.PayBasket(shopperId)
      probe.expectMsg(Wallet.Paid(List(displays, macPro, appleMouse), shopperId))

      shopper ! Basket.Add(appleKeyboard, shopperId)
      shopper ! Shopper.PayBasket(shopperId)
      probe.expectMsg(Wallet.Paid(List(appleKeyboard), shopperId))
      shopper ! Basket.GetItems(shopperId)
      expectMsg(Items())

      killActors(shopper)
    }

    "start with an empty basket after payment, wallet intact" in {
      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems(shopperId)
      expectMsg(Items())

      shopperResurrected ! Wallet.SpentHowMuch(shopperId)

      expectMsg(expectedTotalSpend)
      killActors(shopperResurrected)
    }

    "not be able to spend more than the cash in the pocket" in {
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[Wallet.NotEnoughCash])

      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.Add(dWave, shopperId)
      shopperResurrected ! Shopper.PayBasket(shopperId)

      val left = Shopper.cash - expectedTotalSpend.amount
      probe.expectMsg(Wallet.NotEnoughCash(left))

      shopperResurrected ! Wallet.Check(shopperId)
      expectMsg(Wallet.Cash(left))

      killActors(shopperResurrected)
    }
  }
}
