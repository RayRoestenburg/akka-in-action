package com.manning.chat

import akka.actor._

import Rooms._
import Room._

object User {
  def props(worker: ActorRef, rooms: ActorRef) = Props[User](new User(worker, rooms))
}

class User(worker: ActorRef, rooms: ActorRef) extends Actor {
  def receive = {
    case s: Send        ⇒ rooms ! s
    case m: RoomMessage ⇒ //worker ! Message(m.sender.path.name, m.room, m.content)
  }
}
