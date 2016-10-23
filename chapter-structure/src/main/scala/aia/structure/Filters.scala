package aia.structure

import akka.actor.{ Actor, ActorRef }


case class Photo(license: String, speed: Int)

class SpeedFilter(minSpeed: Int, pipe: ActorRef) extends Actor {
  def receive = {
    case msg: Photo =>
      if (msg.speed > minSpeed)
        pipe ! msg
  }
}

class LicenseFilter(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: Photo =>
      if (!msg.license.isEmpty)
        pipe ! msg
  }
}
