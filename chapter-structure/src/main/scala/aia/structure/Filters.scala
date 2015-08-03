package aia.structure

import akka.actor.{ Actor, ActorRef }

//<start id="ch7-pipe"/>
case class Photo(license: String, speed: Int) //<co id="ch07-pipe-1" />

class SpeedFilter(minSpeed: Int, pipe: ActorRef) extends Actor {
  def receive = {
    case msg: Photo =>
      if (msg.speed > minSpeed) //<co id="ch07-pipe-2" />
        pipe ! msg
  }
}

class LicenseFilter(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: Photo =>
      if (!msg.license.isEmpty) //<co id="ch07-pipe-3" />
        pipe ! msg
  }
}
//<end id="ch7-pipe"/>