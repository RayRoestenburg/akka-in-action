package com.manning.chat

import akka.testkit._
import akka.actor._

class RoomSpec extends AkkaSpec(PersistenceSpec.config("leveldb", "RoomSpec"))
    with PersistenceSpec with ImplicitSender {

  import Room._

  system.eventStream.subscribe(testActor, classOf[MessagePosted])
  system.eventStream.subscribe(testActor, classOf[UserLeft])
  system.eventStream.subscribe(testActor, classOf[UserJoined])

  "The Room actor" should {
    "allow users to join" in {
      val room = system.actorOf(Room.props, "room1")

      room ! Join("Ray", testActor)
      room ! Join("Ray", testActor)

      expectMsg(UserJoined("Ray", testActor))
    }

    "allow users to leave" in {
      val room = system.actorOf(Room.props, "room2")

      room ! Leave("Ray", testActor)
      room ! Join("Ray", testActor)
      room ! Leave("Ray", testActor)

      expectMsg(UserJoined("Ray", testActor))
      expectMsg(UserLeft("Ray", testActor))
    }

    "receive messages from users and store these in a persistent journal" in {
      val room = system.actorOf(Room.props, "room3")

      room ! Join("Ray", testActor)
      expectMsg(UserJoined("Ray", testActor))

      val msg = RoomMessage("Hey it's me", "room3", testActor)

      room ! msg
      expectMsg(msg)
      expectMsg(MessagePosted(msg))

      watch(room)
      system.stop(room)

      expectMsgPF() {
        case Terminated(room) ⇒
          val newRoom = system.actorOf(Room.props, "room3")
          newRoom ! GetMessageLog
          expectMsg(MessageLog(Vector(msg)))
      }
    }
  }
}