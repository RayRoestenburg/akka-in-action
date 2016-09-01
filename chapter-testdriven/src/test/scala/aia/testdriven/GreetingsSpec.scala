//package aia.testdriven
//
//import org.scalatest.matchers.MustMatchers
//import org.scalatest.{BeforeAndAfterAll, WordSpec}
//import akka.testkit.TestKit
//import akka.actor._
//import akka.pattern.ask
//import akka.util.duration._
//import akka.util.Timeout
//
//class GreetingsSpec extends TestKit(ActorSystem("testgreetings")) with WordSpec with MustMatchers with BeforeAndAfterAll {
//  implicit val timeout = Timeout(1 second)
//  "A greeting actor " must {
//    "respond to a greeting and state it's name" in {
//      val greetingActorRef = system.actorOf(Props(new GreetingActor(name = "Alice")))
//      val response = greetingActorRef.ask(Greeting("Bob"))
//      //      response.mapTo[Greeting].onComplete {
//      //
//      //      }
//    }
//    "be able to receive a greeting and log it, version 1" in {
//      val greetingActorRef = system.actorOf(Props[GreetingActor1])
//      greetingActorRef ! Greeting("Bob")
//      // sending is asynchronous, how do we know it is done?
//    }
//
//    "be able to receive a greeting and log it, version 2" in {
//      val greetingActorRef = system.actorOf(Props(new GreetingActor1 with WireTap))
//      greetingActorRef ! Greeting("Bob")
//      // how do we know it is done?
//      expectMsg(Greeting("Bob"))
//    }
//  }
//
//  override protected def afterAll(): Unit = {
//    system.terminate()
//  }
//
//  trait WireTap extends Actor {
//    abstract override def receive = {
//      case m =>
//        super.receive(m)
//        testActor ! m
//    }
//  }
//
//}
//
//case class Greeting(name: String, greeting: String = "Hello")
//
//case class GetGreetings()
//
//class GreetingActor(val name: String) extends Actor {
//  def receive = {
//    case msg: Greeting => sender() ! Greeting(name, "Hello %s" format (msg.name))
//  }
//}
//
//
//class GreetingActor1 extends Actor with ActorLogging {
//  def receive = {
//    case msg: Greeting => log.info("Hello %s" format msg.name)
//  }
//}
//
//class GreetingActor2(listener: ActorRef) extends Actor with ActorLogging {
//  def receive = {
//    case msg: Greeting =>
//      val printMessage = "Hello %s" format msg.name
//      log.info(printMessage)
//      listener ! printMessage
//  }
//}
//
//class GreetingActor3 extends Actor with ActorLogging {
//  private var receivedGreetings = Vector[Greeting]()
//
//  def receive = {
//    case msg: Greeting =>
//      log.info("Hello %s" format msg.name)
//      receivedGreetings = receivedGreetings :+ msg
//    case msg: GetGreetings =>
//      sender() ! receivedGreetings
//  }
//}
//
