package aia.performance.monitor

import akka.actor.{ ActorRef, ActorSystem }
import akka.dispatch._
import scala.Some
import java.util.Queue
import com.typesafe.config.Config
import java.util.concurrent.ConcurrentLinkedQueue
import akka.dispatch.{ MailboxType, MessageQueue, UnboundedMessageQueueSemantics }
import akka.event.LoggerMessageQueueSemantics

case class MonitorEnvelope(queueSize: Int,
                           receiver: String,
                           entryTime: Long,
                           handle: Envelope)

case class MailboxStatistics(queueSize: Int,
                             receiver: String,
                             sender: String,
                             entryTime: Long,
                             exitTime: Long)




class MonitorQueue(val system: ActorSystem)
    extends MessageQueue
    with UnboundedMessageQueueSemantics
    with LoggerMessageQueueSemantics {
  private final val queue = new ConcurrentLinkedQueue[MonitorEnvelope]()



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
        case stat: MailboxStatistics => //skip message <co id="ch14-mailbox-dequeue-1" />
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



class MonitorMailboxType(settings: ActorSystem.Settings, config: Config)
    extends MailboxType 
    with ProducesMessageQueue[MonitorQueue]{

  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    system match {
      case Some(sys) =>
        new MonitorQueue(sys)
      case _ =>
        throw new IllegalArgumentException("requires a system")
    }
  }
}

