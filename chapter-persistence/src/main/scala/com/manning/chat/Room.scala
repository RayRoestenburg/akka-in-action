package com.manning.chat

import akka.actor._
import akka.persistence._

object Room {
  def props = Props[Room]

  case class Join(username: String, userRef: ActorRef)
  case class Leave(username: String, userRef: ActorRef)
  case class RoomMessage(content: String, room: String, sender: ActorRef)
  case object GetMessageLog

  sealed trait RoomEvent
  case class UserJoined(username: String, userRef: ActorRef) extends RoomEvent
  case class UserLeft(username: String, userRef: ActorRef) extends RoomEvent
  case class MessagePosted(message: RoomMessage) extends RoomEvent

  case class MessageLog(messages: Vector[RoomMessage] = Vector()) {
    def update(msg: RoomMessage) = copy(messages :+ msg)
    def size = messages.size
    override def toString: String = messages.toString
  }
}

class Room extends EventsourcedProcessor {
  import Room._

  var messageLog = MessageLog()
  var users = Set[ActorRef]()

  override def processorId = self.path.name

  def receiveCommand = {
    case Join(username, userRef) ⇒
      users.find(_ == userRef) match {
        case None ⇒
          users = users + userRef
          context.system.eventStream.publish(UserJoined(username, userRef))
          messageLog.messages.foreach(m ⇒ users.foreach(_ ! m))
        case _ ⇒
      }

    case m: RoomMessage ⇒
      persist(m) { message ⇒
        addToMessageLog(message)
        users.foreach(_ ! m)
        context.system.eventStream.publish(MessagePosted(m))
      }
    case GetMessageLog ⇒ sender ! messageLog

    case Leave(username, userRef) ⇒
      users.find(_ == userRef).map { userRef ⇒
        users = users - userRef
        context.system.eventStream.publish(UserLeft(username, userRef))
      }
  }

  def addToMessageLog(message: RoomMessage): Unit = messageLog = messageLog.update(message)

  val receiveRecover: Receive = {
    case msg: RoomMessage                       ⇒ addToMessageLog(msg)
    case SnapshotOffer(_, snapshot: MessageLog) ⇒ messageLog = snapshot
  }
}
