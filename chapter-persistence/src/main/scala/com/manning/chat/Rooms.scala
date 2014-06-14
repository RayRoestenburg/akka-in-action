package com.manning.chat

import akka.actor._
import akka.persistence._

object Rooms {
  def props = Props[Rooms]

  case class CreateRoom(name: String)
  case class FindRoom(name: String)
  case object ListRooms
  case class RoomRef(actorRef: ActorRef)
  case class RoomCreated(name: String)
  case class RoomsList(rooms: Vector[String] = Vector()) {
    def update(room: String) = copy(rooms :+ room)
  }

  case class Login(user: String)
  case class LoginSession(user: String, worker: ActorRef)
  case class Send(content: String, room: String)
  case class UserLoggedIn(user: ActorRef, username: String)
  case class JoinRoom(user: String, name: String)
  case class LeaveRoom(user: String, name: String)
}

class Rooms extends EventsourcedProcessor {
  import Rooms._
  import Room._

  override def processorId = "the_rooms"

  var roomsList = RoomsList()

  def receiveCommand = {
    case c: CreateRoom ⇒
      persist(c) { persisted ⇒
        createRoom(persisted.name)
        addToRoomsList(persisted.name)
        context.system.eventStream.publish(RoomCreated(persisted.name))
      }
    case FindRoom(name) ⇒ sender() ! context.child(name).map(RoomRef).getOrElse(RoomRef(createRoom(name)))
    case ListRooms      ⇒ sender() ! roomsList.rooms
    case Send(content, roomName) ⇒ context.child(roomName).map { room ⇒
      room ! RoomMessage(content, roomName, sender())
    }
    case JoinRoom(userName, room) ⇒ context.child(room).map { room ⇒
      context.child(userName).map { user ⇒
        room ! Join(userName, user)
      }
    }
    case LoginSession(user, worker) ⇒
      val userRef = context.actorOf(User.props(worker, self), user)
      context.system.eventStream.publish(UserLoggedIn(userRef, user))
  }

  val receiveRecover: Receive = {
    case c: CreateRoom ⇒
      addToRoomsList(c.name)
      context.actorOf(Room.props, c.name)
    case SnapshotOffer(_, snapshot: RoomsList) ⇒
      roomsList = snapshot
      roomsList.rooms.foreach(room ⇒ context.actorOf(Room.props, room))
  }

  def createRoom(name: String) = context.actorOf(Room.props, name)
  def addToRoomsList(room: String): Unit = roomsList = roomsList.update(room)
}
