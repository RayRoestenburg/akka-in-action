package aia.persistence

import scala.concurrent.duration._
import akka.actor._
import akka.testkit._
import org.scalatest._

class ShopperSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val shopperId = 1L
  val shopperName = s"shopper-$shopperId"
  val macbookPro = Basket.Item("Apple Macbook Pro 15 inch", 1, BigDecimal(2499.99))
  val macPro = Basket.Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Basket.Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Basket.Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Basket.Item("Apple Bluetooth Keyboard", 1, BigDecimal(79.99))

  "The shopper" should {
    "be able to put items in the shopping basket and view the basket" in {
      val shopper = system.actorOf(Shopper.props(shopperId), shopperName)
      shopper ! Basket.Add(macbookPro)
      shopper ! Basket.Add(displays)
      shopper ! Basket.Add(macPro)
      shopper ! Basket.Remove(macbookPro)
      shopper ! Basket.GetItems
      expectMsg(Basket.Items(displays, macPro))
      killActors(shopper)

      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems
      expectMsg(Basket.Items(displays, macPro))
      killActors(shopperResurrected)
    }

    "be able to pay for items in the shopping basket and view the payment history" in {
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[Wallet.Paid])

      val shopper = system.actorOf(Shopper.props(shopperId), shopperName)
      shopper ! Basket.Add(appleMouse)
      shopper ! Basket.GetItems
      expectMsg(Basket.Items(displays, macPro, appleMouse))
      shopper ! Shopper.PayBasket
      probe.expectMsg(Wallet.Paid(Basket.Items(displays, macPro, appleMouse)))

      shopper ! Basket.Add(appleKeyboard)
      shopper ! Shopper.PayBasket
      probe.expectMsg(Wallet.Paid(Basket.Items(appleKeyboard)))

      val paymentHistory = system.actorOf(PaymentHistory.props(shopperId),
        PaymentHistory.name(shopperId))
      paymentHistory ! PaymentHistory.GetHistory

      expectMsg(PaymentHistory.History(
        List(appleKeyboard, displays, macPro, appleMouse)
      ))

      killActors(shopper, paymentHistory)
    }

    "start with an empty basket after payment, history intact" in {
      val shopperResurrected = system.actorOf(Shopper.props(shopperId),
        shopperName)
      shopperResurrected ! Basket.GetItems
      expectMsg(Basket.Items())

      val paymentHistory = system.actorOf(PaymentHistory.props(shopperId),
        PaymentHistory.name(shopperId))
      paymentHistory ! PaymentHistory.GetHistory

      expectMsg(PaymentHistory.History(
        List(appleKeyboard, displays, macPro, appleMouse)
      ))

      killActors(shopperResurrected, paymentHistory)
     }
  }
}
