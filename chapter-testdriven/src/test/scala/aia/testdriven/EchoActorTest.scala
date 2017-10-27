package aia.testdriven

import akka.testkit.{ TestKit, ImplicitSender }
import akka.actor.{ Props, Actor, ActorSystem }
import org.scalatest.WordSpecLike

import akka.util.Timeout
import scala.concurrent.Await
import scala.util.{ Success, Failure }

import scala.language.postfixOps


class EchoActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with ImplicitSender
  with StopSystemAfterAll {


  "An EchoActor" must {
    "Reply with the same message it receives" in {

      import akka.pattern.ask
      import scala.concurrent.duration._
      implicit val timeout = Timeout(3 seconds)
      implicit val ec = system.dispatcher
      val echo = system.actorOf(Props[EchoActor])
      val future = echo.ask("some message")
      future.onComplete {
        case Failure(_)   => // 失敗時の制御
        case Success(msg) => // 成功時の制御
      }

      Await.ready(future, timeout.duration)
    }

    "Reply with the same message it receives without ask" in {
      val echo = system.actorOf(Props[EchoActor])
      echo ! "some message"
      expectMsg("some message")

    }

  }
}


class EchoActor extends Actor {
  def receive = {
    case msg =>
      sender() ! msg
  }
}

