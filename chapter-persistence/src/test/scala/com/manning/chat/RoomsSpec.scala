package com.manning.chat

import akka.testkit._
import akka.actor._

class RoomsSpec extends AkkaSpec(PersistenceSpec.config("leveldb", "RoomsSpec"))
    with PersistenceSpec with ImplicitSender {

  import Rooms._
  import Room._

  system.eventStream.subscribe(testActor, classOf[RoomCreated])
  system.eventStream.subscribe(testActor, classOf[MessagePosted])
  system.eventStream.subscribe(testActor, classOf[UserLoggedIn])
  system.eventStream.subscribe(testActor, classOf[UserJoined])

  "The Rooms actor" should {
    "create rooms" in {
      val rooms = system.actorOf(Rooms.props, "the_rooms")

      rooms ! CreateRoom("room1")
      expectMsg(RoomCreated("room1"))

      rooms ! CreateRoom("room2")
      expectMsg(RoomCreated("room2"))

      rooms ! ListRooms
      expectMsg(Vector("room1", "room2"))

      watch(rooms)
      system.stop(rooms)
      expectMsgPF() {
        case Terminated(rooms) ⇒
      }
    }

    "recreate rooms from persistence" in {
      val testWebSocket = TestProbe()
      val rooms = system.actorOf(Rooms.props, "the_rooms")
      rooms ! FindRoom("room1")

      expectMsgPF() {
        case RoomRef(room) ⇒

          rooms ! LoginSession("Ray", testWebSocket.testActor)

          expectMsgPF() {
            case UserLoggedIn(user, _) ⇒

              rooms ! JoinRoom("Ray", "room1")

              expectMsgPF() {
                case UserJoined(name, userRef) ⇒
                  userRef ! Send("Hey it's me", "room1")
                  expectMsg(MessagePosted(RoomMessage("Hey it's me", "room1", userRef)))
              }

              watch(rooms)
              system.stop(rooms)
              expectMsgPF() {
                case Terminated(rooms) ⇒
              }

              val resurrectedRooms = system.actorOf(Rooms.props, "the_rooms")

              resurrectedRooms ! FindRoom("room1")

              val roomResurrected = expectMsgPF() {
                case RoomRef(room) ⇒ room
              }

              roomResurrected ! GetMessageLog
              expectMsg(MessageLog(Vector(RoomMessage("Hey it's me", "room1", user))))

              resurrectedRooms ! LoginSession("Ray", testWebSocket.testActor)

              expectMsgPF() {
                case UserLoggedIn(user, _) ⇒

                  resurrectedRooms ! JoinRoom("Ray", "room1")

                  expectMsgPF() {
                    case UserJoined(name, userRef) ⇒
                      userRef ! Send("Hey it's me again", "room1")
                      expectMsg(MessagePosted(RoomMessage("Hey it's me again", "room1", userRef)))
                  }
              }
          }
      }
    }
  }
}