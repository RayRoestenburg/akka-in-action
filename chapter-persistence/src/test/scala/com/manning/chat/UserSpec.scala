package com.manning.chat

import akka.testkit._
import akka.actor._

class UserSpec extends AkkaSpec(PersistenceSpec.config("leveldb", "UserSpec"))
    with PersistenceSpec with ImplicitSender {

  import User._
  import Rooms._
  import Room._

  system.eventStream.subscribe(testActor, classOf[UserLoggedIn])
  system.eventStream.subscribe(testActor, classOf[RoomCreated])

  "The User actor" should {
    "be able to send messages to a room" in {
      val testWebSocket = TestProbe()

      val rooms = system.actorOf(Rooms.props, "the_rooms")
      rooms ! CreateRoom("room1")
      expectMsg(RoomCreated("room1"))

      rooms ! LoginSession("Ray", testWebSocket.testActor)

      expectMsgPF() {
        case UserLoggedIn(user, _) ⇒
          user ! Send("Hey It's me", "room1")
      }
    }

    "reconnect to the room and see all the messages" in {
    }
  }
}