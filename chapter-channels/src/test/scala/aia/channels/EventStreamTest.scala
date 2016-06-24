package aia.channels

import akka.testkit.{ TestProbe, TestKit }
import akka.actor.ActorSystem
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import java.util.Date
import scala.concurrent.duration._

class CancelOrder(time: Date,
                  override val customerId: String,
                  override val productId: String,
                  override val number: Int)
  extends Order(customerId, productId, number)

class EventStreamTest extends TestKit(ActorSystem("EventStreamTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {

  override def afterAll() {
    system.terminate()
  }

  "EventStream" must {
    "distribute messages" in {
      val deliverOrder = TestProbe()
      val giftModule = TestProbe()

      system.eventStream.subscribe(
        deliverOrder.ref,
        classOf[Order])
      system.eventStream.subscribe(
        giftModule.ref,
        classOf[Order])

      val msg = new Order(
        customerId = "me",
        productId = "Akka in Action",
        number = 2)
      system.eventStream.publish(msg)

      deliverOrder.expectMsg(msg)
      giftModule.expectMsg(msg)

    }
    "monitor hierarchy" in {
      val giftModule = TestProbe()

      system.eventStream.subscribe(
        giftModule.ref,
        classOf[Order])

      val msg = new Order("me", "Akka in Action", 3)
      system.eventStream.publish(msg)

      giftModule.expectMsg(msg)

      val msg2 = new CancelOrder(new Date(), "me", "Akka in Action", 2)
      system.eventStream.publish(msg2)

      giftModule.expectMsg(msg2)
    }
    "Ignore other messages" in {
      val giftModule = TestProbe()

      system.eventStream.subscribe(
        giftModule.ref,
        classOf[CancelOrder])
      val msg = new Order("me", "Akka in Action", 3)
      system.eventStream.publish(msg)
      giftModule.expectNoMsg(3 seconds)

    }
    "unscribe messages" in {
      //<start id="ch09-eventStream-test"/>
      val DeliverOrder = TestProbe() //<co id="ch09-eventStream-test-1"/>
      val giftModule = TestProbe() //<co id="ch09-eventStream-test-2"/>

      system.eventStream.subscribe( //<co id="ch09-eventStream-test-3"/>
        DeliverOrder.ref,
        classOf[Order])
      system.eventStream.subscribe( //<co id="ch09-eventStream-test-4"/>
        giftModule.ref,
        classOf[Order])

      val msg = new Order("me", "Akka in Action", 3)
      system.eventStream.publish(msg) //<co id="ch09-eventStream-test-5"/>

      DeliverOrder.expectMsg(msg) //<co id="ch09-eventStream-test-6"/>
      giftModule.expectMsg(msg) //<co id="ch09-eventStream-test-7"/>

      system.eventStream.unsubscribe(giftModule.ref) //<co id="ch09-eventStream-test-8"/>

      system.eventStream.publish(msg)
      DeliverOrder.expectMsg(msg)
      giftModule.expectNoMsg(3 seconds) //<co id="ch09-eventStream-test-9"/>
      //<end id="ch09-eventStream-test"/>
    }
  }
  "MyEventBus" must {
    "deliver all messages" in {
      val bus = new MyEventBus
      val systemLog = TestProbe()
      bus.subscribe(systemLog.ref)
      val msg = new Order("me", "Akka in Action", 3)
      bus.publish(msg)
      systemLog.expectMsg(msg)

      bus.publish("test")
      systemLog.expectMsg("test")

    }
  }
  "OrderMessageBus" must {
    "deliver Order messages" in {
      //<start id="ch09-eventBus-test"/>
      val bus = new OrderMessageBus //<co id="ch09-eventBus-test-1"/>

      val singleBooks = TestProbe()
      bus.subscribe(singleBooks.ref, false) //<co id="ch09-eventBus-test-2"/>
      val multiBooks = TestProbe()
      bus.subscribe(multiBooks.ref, true) //<co id="ch09-eventBus-test-3"/>

      val msg = new Order("me", "Akka in Action", 1)
      bus.publish(msg) //<co id="ch09-eventBus-test-4"/>
      singleBooks.expectMsg(msg)
      multiBooks.expectNoMsg(3 seconds) //<co id="ch09-eventBus-test-5"/>

      val msg2 = new Order("me", "Akka in Action", 3)
      bus.publish(msg2) //<co id="ch09-eventBus-test-6"/>
      singleBooks.expectNoMsg(3 seconds)
      multiBooks.expectMsg(msg2)
      //<end id="ch09-eventBus-test"/>
    }
    "deliver order messages when multiple subscriber" in {
      val bus = new OrderMessageBus
      val listener = TestProbe()
      bus.subscribe(listener.ref, true)
      bus.subscribe(listener.ref, false)

      val msg = new Order("me", "Akka in Action", 1)
      bus.publish(msg)
      listener.expectMsg(msg)

      val msg2 = new Order("me", "Akka in Action", 3)
      bus.publish(msg2)
      listener.expectMsg(msg2)
    }
  }
}
