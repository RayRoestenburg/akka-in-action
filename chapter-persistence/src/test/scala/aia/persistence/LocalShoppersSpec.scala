package aia.persistence

import scala.concurrent.duration._
import akka.actor._
import akka.testkit._
import org.scalatest._

class LocalShoppersSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val macbookPro = Basket.Item("Apple Macbook Pro 15 inch", 1, BigDecimal(2499.99))
  val macPro = Basket.Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Basket.Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Basket.Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Basket.Item("Apple Bluetooth Keyboard", 1, BigDecimal(79.99))

  val expectedTotalSpend = Wallet.AmountSpent(
    (macPro.price * macPro.number) +
    (displays.price * displays.number) +
    (appleMouse.price * appleMouse.number) +
    (appleKeyboard.price * appleKeyboard.number)
  )

  val dWave = Basket.Item("D-Wave One Computer System", 1,
    BigDecimal(14999999.99))

  "The local shoppers" should {
    "forward to the specific shopper" in {
      val probe = TestProbe()

      system.eventStream.subscribe(probe.ref, classOf[Wallet.Paid])

      val shoppers = system.actorOf(LocalShoppers.props, LocalShoppers.name)
      shoppers ! Basket.Add(appleMouse, 1)
      shoppers ! Basket.GetItems(1)
      expectMsg(Basket.Items(appleMouse))

      shoppers ! Basket.Add(displays, 2)
      shoppers ! Basket.GetItems(2)
      expectMsg(Basket.Items(displays))

      shoppers ! Shopper.PayBasket(1)
      probe.expectMsg(Wallet.Paid(List(appleMouse), 1))

      shoppers ! Shopper.PayBasket(2)
      probe.expectMsg(Wallet.Paid(List(displays), 2))

      killActors(shoppers)
    }
  }
}
