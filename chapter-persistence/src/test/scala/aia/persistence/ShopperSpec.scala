package aia.persistence

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._
import org.scalatest._

class ShopperSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val shopperId = 1L
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

    "pay for items in the shopping basket" in { // and view the payment history" in {
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

      // val paymentHistory = system.actorOf(PaymentHistory.props(shopperId),
      //   PaymentHistory.name(shopperId))
      // paymentHistory ! PaymentHistory.GetHistory

      // expectMsg(PaymentHistory.History(
      //   List(appleKeyboard, displays, macPro, appleMouse)
      // ))

      killActors(shopper)//, paymentHistory)
    }

    "start with an empty basket after payment, wallet and history intact" in {
      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems(shopperId)
      expectMsg(Items())

      // val paymentHistory = system.actorOf(PaymentHistory.props(shopperId),
      //   PaymentHistory.name(shopperId))
      // paymentHistory ! PaymentHistory.GetHistory

      // expectMsg(PaymentHistory.History(
      //   List(appleKeyboard, displays, macPro, appleMouse)
      // ))

      shopperResurrected ! Wallet.SpentHowMuch(shopperId)

      expectMsg(expectedTotalSpend)
      killActors(shopperResurrected)//, paymentHistory)
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

      // val paymentHistory = system.actorOf(PaymentHistory.props(shopperId),
      //   PaymentHistory.name(shopperId))
      // paymentHistory ! PaymentHistory.GetHistory

      // expectMsg(PaymentHistory.History(
      //   List(appleKeyboard, displays, macPro, appleMouse)
      // ))

      killActors(shopperResurrected)//, paymentHistory)
     }
  }
}
