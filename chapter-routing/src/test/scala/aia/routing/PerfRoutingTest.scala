package aia.routing

import java.util.Date
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import akka.routing._
import scala.concurrent.Await

import org.scalatest.{MustMatchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit._

class PerfRoutingTest
  extends TestKit(ActorSystem("PerfRoutingTest"))
  with MustMatchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The routerGroup" must {
    "use manage routees" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "router")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props( new DynamicRouteeSizer(2, props, router)),"DynamicRouteeSizer")
      Thread.sleep(100)
      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      router ! msg

      val procMsg = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      creator ! PreferredSize(4)
      Thread.sleep(1000)

      creator ! PreferredSize(2)
      Thread.sleep(1000)

      router ! msg
      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg2)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      router ! Broadcast(PoisonPill)
      Thread.sleep(1000)

      router ! msg
      val procMsg3 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg3)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      system.stop(router)
      system.stop(creator)
    }
    "survive killed actor ref routee" in {
      val termProbe = TestProbe()
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "router-test2")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props(new WrongDynamicRouteeSizer(2, props, router)),"DynamicRouteeSizer-test2")
      Thread.sleep(100)

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      val routee = routees.get(0).asInstanceOf[ActorRefRoutee]
      termProbe.watch(routee.ref)
      termProbe.watch(router)
      routee.send(PoisonPill, endProbe.ref)
      termProbe.expectTerminated(routee.ref)
      termProbe.expectNoMsg
      termProbe.unwatch(router)
      system.stop(creator)
    }
    "survive killed routee" in {
      val termProbe = TestProbe()
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "router-test3")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props( new DynamicRouteeSizer(2, props, router)),"DynamicRouteeSizer-test3")
      Thread.sleep(100)
      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      routees.get(0).send(PoisonPill, endProbe.ref)

      termProbe.watch(router)
      //// router isn't terminated due to the use of ActorSelectionRoutee
      termProbe.expectNoMsg
      termProbe.unwatch(router)

      val future2 = router.ask(GetRoutees)(1 second)
      val routeesMsg2 = Await.result(future2, 1.second).asInstanceOf[Routees]
      routeesMsg2.getRoutees.size must be (2)
      import collection.JavaConversions._
      for(routee <- routeesMsg2.getRoutees) {
        routees.get(0).send(msg, endProbe.ref)
      }
      val procMsg1 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg1)
      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg2)
      system.stop(router)
      system.stop(creator)
    }

    "use manage routees2" in {
      val termProbe = TestProbe()
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "router-test4")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props( new DynamicRouteeSizer2(2, props, router)),"DynamicRouteeSizer2-test4")
      Thread.sleep(100)
      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      router ! msg

      val procMsg = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      creator ! PreferredSize(4)
      Thread.sleep(1000)

      creator ! PreferredSize(2)
      Thread.sleep(2000)

      router ! msg
      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg2)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      router ! Broadcast(PoisonPill)

      termProbe.watch(router)
      termProbe.expectTerminated(router)  
      
      system.stop(router)
      system.stop(creator)
    }

    "Use recreated routees" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val creator = system.actorOf(Props( new GetLicenseCreator2(2, endProbe.ref)),"GetLicenseCreator-test5")
      val paths = List(
        "/user/GetLicenseCreator-test5/GetLicense0",
        "/user/GetLicenseCreator-test5/GetLicense1"
      )
      val router = system.actorOf(RoundRobinGroup(paths).props(), "router-test5")

      creator ! "KillFirst"

      Thread.sleep(100)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      router ! msg

      val procMsg = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      creator ! "KillFirst"
      Thread.sleep(100)

      router ! msg

      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg2)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      creator ! "KillFirst"
      Thread.sleep(100)

      router ! msg

      val procMsg3 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg3)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      system.stop(router)
      system.stop(creator)
    }
    "Use recreated routees using broadcast" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val creator = system.actorOf(Props( new GetLicenseCreator2(2, endProbe.ref)),"GetLicenseCreator-test6")
      val paths = List(
        "/user/GetLicenseCreator-test6/GetLicense0",
        "/user/GetLicenseCreator-test6/GetLicense1"
      )
      val router = system.actorOf(RoundRobinGroup(paths).props(), "groupRouter-test6")

      deadProbe.expectNoMsg()
      router ! Broadcast(PoisonPill)
      deadProbe.expectNoMsg()

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)
      router ! msg

      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg2)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      router ! msg

      val procMsg3 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: "+ procMsg3)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()

      system.stop(router)
      system.stop(creator)
    }
    "route between created routers 2" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()
      system.eventStream.subscribe(
        deadProbe.ref,
        classOf[DeadLetter])

      val creator = system.actorOf(Props( new GetLicenseCreator(2, endProbe.ref)),"GetLicenseCreator2-test7")
      val paths = List(
        "/user/GetLicenseCreator2-test7/GetLicense0",
        "/user/GetLicenseCreator2-test7/GetLicense1"
      )
      val router = system.actorOf(RoundRobinGroup(paths).props(), "router-test7")

      creator ! "KillFirst"

      Thread.sleep(1000)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      router ! msg

      val deadMsg = deadProbe.expectMsgType[DeadLetter](1 second)
      println(deadMsg)
      router ! msg

      endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      router ! msg

      deadProbe.expectMsgType[DeadLetter](1 second)
      system.stop(router)
      system.stop(creator)
    }
  }
  "The Router supervision" must {
    "restart all routees" in {
      val router = system.actorOf(
        RoundRobinPool(5).props(
          Props[TestSuper]), "roundrobinRouter-test1")
      router ! "exception"
      Thread.sleep(1000)
      system.stop(router)
    }
    "restart one routee" in {
      val router = system.actorOf(
        RoundRobinPool(5, supervisorStrategy = SupervisorStrategy.defaultStrategy).props(
          Props[TestSuper]), "roundrobinRouter-test2")
      router ! "exception"
      Thread.sleep(1000)
      system.stop(router)
    }
  }
    "The Router" must {
    "routes using roundrobin" in {
      //<start id="ch09-routing-perf-test"/>
      val endProbe = TestProbe()
      val router = system.actorOf(
        RoundRobinPool(5).props(                //<co id="ch09-routing-perf-test-1" />
          Props(new GetLicense(endProbe.ref, 250 millis))), "roundrobinRouter")

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      for (index <- 0 until 10) { //<co id="ch09-routing-perf-test-2" />
        router ! msg
      }
      val processedMessages = endProbe.receiveN(10, 5 seconds).collect //<co id="ch09-routing-perf-test-3" />
      { case m: PerformanceRoutingMessage => m }
      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy) //<co id="ch09-routing-perf-test-4" />
      grouped.values.foreach(listProcessedByOneActor =>
        listProcessedByOneActor must have size (2)) //<co id="ch09-routing-perf-test-5" />
      //<end id="ch09-routing-perf-test"/>
      system.stop(router)
    }
    "routes using smallest mailbox" in {
      //<start id="ch09-routing-mailbox-test"/>
      val endProbe = TestProbe()

      val router = system.actorOf(SmallestMailboxPool(2).props(Props(
        new GetLicense(endProbe.ref))), "smallestMailboxRouter-test1")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      routees.size must be (2)
      routees.get(0).send(SetService("250", 250 millis), endProbe.ref)
      routees.get(1).send(SetService("500", 500 millis), endProbe.ref)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      for (index <- 0 until 10) {
        Thread.sleep(200) //<co id="ch09-routing-perf-test3-1" />
        router ! msg
      }
      val processedMessages = endProbe.receiveN(10, 5 seconds).collect { case m: PerformanceRoutingMessage => m }
      processedMessages.size must be(10)
      val grouped = processedMessages.groupBy(_.processedBy)
      val msgProcessedByActor1 = grouped.get(Some("250"))
        .getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500"))
        .getOrElse(Seq())
      msgProcessedByActor1 must have size (7) //<co id="ch09-routing-perf-test3-2" />
      msgProcessedByActor2 must have size (3) //<co id="ch09-routing-perf-test3-3" />
      //<end id="ch09-routing-mailbox-test"/>
      system.stop(router)
    }
    "routes using smallest mailbox with addRoutee" in {
      //<start id="ch09-routing-mailbox2-test"/>
      val endProbe = TestProbe()

      val router = system.actorOf(SmallestMailboxPool(0).props(Props(new GetLicense(endProbe.ref, 250 millis))), "smallestMailboxRouter-test2")

      val actor1 = system.actorOf(Props(new GetLicense(endProbe.ref, 250 millis)),"250")
      val actor2 = system.actorOf(Props(new GetLicense(endProbe.ref, 500 millis)),"500")
      router ! AddRoutee(ActorRefRoutee(actor1))
      router ! AddRoutee(ActorRefRoutee(actor2))

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      routees.size must be (2)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      for (index <- 0 until 10) {
        Thread.sleep(200) //<co id="ch09-routing-perf-test3b-1" />
        router ! msg
      }
      val processedMessages = endProbe.receiveN(10, 5 seconds).collect { case m: PerformanceRoutingMessage => m }
      processedMessages.size must be(10)
      val grouped = processedMessages.groupBy(_.processedBy)
      val msgProcessedByActor1 = grouped.get(Some("250"))
        .getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500"))
        .getOrElse(Seq())
      msgProcessedByActor1 must have size (7) //<co id="ch09-routing-perf-test3b-2" />
      msgProcessedByActor2 must have size (3) //<co id="ch09-routing-perf-test3b-3" />
      //<end id="ch09-routing-mailbox2-test"/>
      system.stop(router)
    }
    "routes using balancedRouter" in {
      //<start id="ch09-routing-dispatcher-test"/>
      val testSystem = ActorSystem("balancedRouter")
      val endProbe = TestProbe()(testSystem)

      val router = system.actorOf(BalancingPool(2).props(Props(
        new GetLicense(endProbe.ref))), "BalancingPoolRouter")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      routees.size must be (2)
      routees.get(0).send(SetService("250", 250 millis), endProbe.ref)
      routees.get(1).send(SetService("500", 500 millis), endProbe.ref)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      for (index <- 0 until 10) {
        router ! msg
      }
      val processedMessages = endProbe.receiveN(10, 5 seconds).collect { case m: PerformanceRoutingMessage => m }
      processedMessages.size must be(10)
      println(processedMessages)
      val grouped = processedMessages.groupBy(_.processedBy)
      val msgProcessedByActor1 = grouped.get(Some("250"))
        .getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500"))
        .getOrElse(Seq())

      msgProcessedByActor1.size must be(7 +- 1) //<co id="ch09-routing-perf-test4-4" />
      msgProcessedByActor2.size must be(3 +- 1) //<co id="ch09-routing-perf-test4-5" />
      testSystem.terminate()
      //<end id="ch09-routing-dispatcher-test"/>
      system.stop(router)
    }
    "create routes using BalancingPool and using direct" in {
      //<start id="ch09-routing-dispatch-test"/>
      val testSystem = ActorSystem("balance",
        ConfigFactory.load("balance"))
      val endProbe = TestProbe()(testSystem)
      val router = system.actorOf(BalancingPool(2).props(Props(
        new GetLicense(endProbe.ref))), "BalancingPoolRouter2")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees
      routees.size must be (2)
      routees.get(0).send(SetService("250", 250 millis), endProbe.ref)
      routees.get(1).send(SetService("500", 500 millis), endProbe.ref)

      val msg = PerformanceRoutingMessage(
        ImageProcessing.createPhotoString(new Date(), 60, "123xyz"),
        None,
        None)

      for (index <- 0 until 10) {
        routees.get(0).send(msg, endProbe.ref)
      }
      val processedMessages = endProbe.receiveN(10, 5 seconds).collect { case m: PerformanceRoutingMessage => m }
      processedMessages.size must be(10)
      val grouped = processedMessages.groupBy(_.processedBy)
      val msgProcessedByActor1 = grouped.get(Some("250"))
        .getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500"))
        .getOrElse(Seq())
      msgProcessedByActor1.size must be(7 +- 1)
      msgProcessedByActor2.size must be(3 +- 1)
      testSystem.terminate()
      //<end id="ch09-routing-dispatch-test"/>
      system.stop(router)
    }
  }
}