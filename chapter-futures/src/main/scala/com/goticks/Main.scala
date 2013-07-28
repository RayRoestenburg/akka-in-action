package com.goticks

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem("edge-server")
  val settings = Settings(system)

  val api = system.actorOf(Props(new TicketInfoServiceActor()), "ticketInfoService")
  IO(Http) ! Http.Bind(listener = api, interface = settings.Http.Host, port = settings.Http.Port)
}
