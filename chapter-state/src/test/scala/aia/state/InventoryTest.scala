package aia.state

import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.actor.FSM.{
  Transition,
  CurrentState,
  SubscribeTransitionCallBack
}
import concurrent.duration._

class InventoryTest extends TestKit(ActorSystem("InventoryTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers
  with ImplicitSender {

  override def afterAll() {
    system.terminate()
  }

  "Inventory" must {
    "follow the flow" in {
      val publisher = system.actorOf(Props(new Publisher(2, 2)))
      val inventory = system.actorOf(Props(new Inventory(publisher)))
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory, WaitForRequests))

      //start test
      inventory ! new BookRequest("context1", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForPublisher, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(
        new BookReply("context1", Right(1)))

      inventory ! new BookRequest("context2", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context2", Right(2)))

      inventory ! new BookRequest("context3", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForPublisher, ProcessSoldOut))
      replyProbe.expectMsg(
        new BookReply("context3", Left("SoldOut")))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessSoldOut, SoldOut))

      inventory ! new BookRequest("context4", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, SoldOut, ProcessSoldOut))
      replyProbe.expectMsg(new BookReply("context4", Left("SoldOut")))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessSoldOut, SoldOut))
      system.stop(inventory)
      system.stop(publisher)
    }
    "process multiple requests" in {
      val publisher = system.actorOf(Props(new Publisher(2, 2)))
      val inventory = system.actorOf(Props(new Inventory(publisher)))
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory, WaitForRequests))

      //start test
      inventory ! new BookRequest("context1", replyProbe.ref)
      inventory ! new BookRequest("context2", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForPublisher, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context1", Right(1)))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context2", Right(2)))

      system.stop(inventory)
      system.stop(publisher)
    }
    "support multiple supplies" in {
      val publisher = system.actorOf(Props(new Publisher(4, 2)))
      val inventory = system.actorOf(Props(new Inventory(publisher)))
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory, WaitForRequests))

      //start test
      inventory ! new BookRequest("context1", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForPublisher, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context1", Right(1)))

      inventory ! new BookRequest("context2", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context2", Right(2)))

      inventory ! new BookRequest("context3", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForPublisher, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context3", Right(3)))

      inventory ! new BookRequest("context4", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, ProcessRequest))
      stateProbe.expectMsg(
        new Transition(inventory, ProcessRequest, WaitForRequests))
      replyProbe.expectMsg(new BookReply("context4", Right(4)))

      system.stop(inventory)
      system.stop(publisher)
    }
  }
  "InventoryTimer" must {
    "follow the flow" in {
      //<start id="ch10-fsm-timer-test"/>
      val publisher = TestProbe()
      val inventory = system.actorOf(
        Props(new InventoryWithTimer(publisher.ref)))
      val stateProbe = TestProbe()
      val replyProbe = TestProbe()

      inventory ! new SubscribeTransitionCallBack(stateProbe.ref)
      stateProbe.expectMsg(
        new CurrentState(inventory, WaitForRequests))

      //start test
      inventory ! new BookRequest("context1", replyProbe.ref)
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      publisher.expectMsg(PublisherRequest)
      stateProbe.expectMsg(6 seconds, //<co id="ch10-fsm-timer-test-1"/>
        new Transition(inventory, WaitForPublisher, WaitForRequests))
      stateProbe.expectMsg(
        new Transition(inventory, WaitForRequests, WaitForPublisher))
      //<end id="ch10-fsm-timer-test"/>
      system.stop(inventory)
    }
  }

}
