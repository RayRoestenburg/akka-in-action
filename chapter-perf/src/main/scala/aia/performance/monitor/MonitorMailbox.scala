package aia.performance.monitor

import akka.actor.{ ActorRef, ActorSystem }
import akka.dispatch._
import scala.Some
import java.util.Queue
import com.typesafe.config.Config
import java.util.concurrent.ConcurrentLinkedQueue
import akka.dispatch.MessageQueue

case class MonitorEnvelope(queueSize: Int,
                           receiver: String,
                           entryTime: Long,
                           handle: Envelope)

case class MailboxStatistics(queueSize: Int,
                             receiver: String,
                             sender: String,
                             entryTime: Long,
                             exitTime: Long)

trait MonitorMailbox extends MessageQueue {
  def system: ActorSystem
  def queue: Queue[MonitorEnvelope]
  def numberOfMessages = queue.size
  def hasMessages = !queue.isEmpty
  def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    if (hasMessages) {
      var envelope = dequeue
      while (envelope ne null) {
        deadLetters.enqueue(owner, envelope)
        envelope = dequeue
      }
    }
  }

  def enqueue(receiver: ActorRef, handle: Envelope): Unit = {

    val env = MonitorEnvelope(queueSize = queue.size() + 1,
      receiver = receiver.toString(),
      entryTime = System.currentTimeMillis(),
      handle = handle)
    queue add env
  }
  def dequeue(): Envelope = {
    val monitor = queue.poll()
    if (monitor != null) {
      monitor.handle.message match {
        case stat: MailboxStatistics => //skip message
        case _ => {
          val stat = MailboxStatistics(
            queueSize = monitor.queueSize,
            receiver = monitor.receiver,
            sender = monitor.handle.sender.toString(),
            entryTime = monitor.entryTime,
            exitTime = System.currentTimeMillis())
          system.eventStream.publish(stat)
        }
      }
      monitor.handle
    } else {
      null
    }
  }
}

//<start id="ch14-mailboxType"/>
class MonitorMailboxType extends akka.dispatch.MailboxType {

  def this(settings: ActorSystem.Settings, config: Config) = this() //<co id="ch14-mailboxType-1" />

  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    system match {
      case Some(sys) =>
        new ConcurrentLinkedQueue[MonitorEnvelope]() //<co id="ch14-mailboxType-2" />
        with MonitorMailbox {
          final def system = sys
          final def queue: Queue[MonitorEnvelope] = this
        }
      case _ =>
        throw new IllegalArgumentException("requires an system") //<co id="ch14-mailboxType-3" />
    }
  }
}
//<end id="ch14-mailboxType"/>