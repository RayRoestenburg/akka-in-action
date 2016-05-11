package aia.persistence.rest

import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import spray.can._
import spray.http._

import aia.persistence._

trait ShoppersServiceSupport {
  def startService(shoppers: ActorRef)(implicit system: ActorSystem) = {
    val settings = Settings(system)
    val host = settings.http.host
    val port = settings.http.port

    val service = system.actorOf(Props(new ShoppersService(shoppers)),
      "shoppers-service")

    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10 seconds)

    IO(Http).ask(Http.Bind(listener = service, interface = host, port = port))
      .mapTo[Http.Event]
      .map {
        case Http.Bound(address) =>
          println(s"Shopper service bound to $address")
        case Http.CommandFailed(cmd) =>
          println("Shopper service could not bind to " +
            s"$host:$port, ${cmd.failureMessage}")
          system.terminate()
      }
  }
}
