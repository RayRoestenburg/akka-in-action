package aia.persistence

import scala.concurrent.duration._

import akka.actor._
import akka.testkit._
import org.scalatest._

class BasketSpec extends PersistenceSpec(ActorSystem("test"))
    with PersistenceCleanup {

  val shopperId = 2L
  val macbookPro = Item("Apple Macbook Pro", 1, BigDecimal(2499.99))
  val macPro = Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Item("Apple Keyboard", 1, BigDecimal(79.99))
  val dWave = Item("D-Wave One", 1, BigDecimal(14999999.99))

  "The basket" should {
    "skip basket events that occured before Cleared during recovery" in {
      val basket = system.actorOf(Basket.props, Basket.name(shopperId))
      basket ! Basket.Add(macbookPro, shopperId)
      basket ! Basket.Add(displays, shopperId)
      basket ! Basket.GetItems(shopperId)
      expectMsg(Items(macbookPro, displays))

      basket ! Basket.Clear(shopperId) //<co id="cause_snapshot_1"/>

      basket ! Basket.Add(macPro, shopperId)
      basket ! Basket.RemoveItem(macPro.productId, shopperId)
      expectMsg(Some(Basket.ItemRemoved(macPro.productId)))

      basket ! Basket.Clear(shopperId) //<co id="cause_snapshot_2"/>
      basket ! Basket.Add(dWave, shopperId)
      basket ! Basket.Add(displays, shopperId)

      basket ! Basket.GetItems(shopperId)
      expectMsg(Items(dWave, displays))

      killActors(basket)

      val basketResurrected = system.actorOf(Basket.props,
        Basket.name(shopperId))
      basketResurrected ! Basket.GetItems(shopperId)
      expectMsg(Items(dWave, displays))

      basketResurrected ! Basket.CountRecoveredEvents(shopperId)
      expectMsg(Basket.RecoveredEventsCount(2)) //<co id="assert_count_recovered_events"/>

      killActors(basketResurrected)
    }
  }
}
