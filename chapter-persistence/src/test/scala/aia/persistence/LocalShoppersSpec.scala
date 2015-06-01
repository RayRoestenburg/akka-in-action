package aia.persistence

import scala.concurrent.duration._
import akka.actor._
import akka.testkit._
import org.scalatest._

class LocalShoppersSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val macbookPro =
    Basket.Item("Apple Macbook Pro", 1, BigDecimal(2499.99))
  val macPro = Basket.Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Basket.Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Basket.Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Basket.Item("Apple Keyboard", 1, BigDecimal(79.99))

  "The local shoppers" should {
    "forward to the specific shopper" in {
      val probe = TestProbe()

      system.eventStream.subscribe(probe.ref, classOf[Wallet.Paid])

      val shoppers = system.actorOf(LocalShoppers.props, LocalShoppers.name)
      val shopperId1 = 1
      val shopperId2 = 2

      shoppers ! Basket.Add(appleMouse, shopperId1)
      shoppers ! Basket.Add(appleKeyboard, shopperId1)
      shoppers ! Basket.GetItems(shopperId1)
      expectMsg(Basket.Items(appleMouse, appleKeyboard))

      shoppers ! Basket.Add(displays, shopperId2)
      shoppers ! Basket.GetItems(shopperId2)
      expectMsg(Basket.Items(displays))

      shoppers ! Shopper.PayBasket(shopperId1)
      probe.expectMsg(Wallet.Paid(List(appleMouse, appleKeyboard), shopperId1))

      shoppers ! Shopper.PayBasket(shopperId2)
      probe.expectMsg(Wallet.Paid(List(displays), shopperId2))

      killActors(shoppers)
    }
  }
}
