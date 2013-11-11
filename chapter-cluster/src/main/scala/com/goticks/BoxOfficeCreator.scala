package com.goticks

import akka.actor._

trait BoxOfficeCreator { this: Actor =>
  def createBoxOffice:ActorRef = context.actorOf(Props[BoxOffice],"boxOffice")
}
