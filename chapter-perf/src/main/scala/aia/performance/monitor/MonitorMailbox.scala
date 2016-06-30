package aia.performance.monitor

import akka.actor.{ ActorRef, ActorSystem }
import akka.dispatch._
import scala.Some
import java.util.Queue
import com.typesafe.config.Config
import java.util.concurrent.ConcurrentLinkedQueue
import akka.dispatch.{ MailboxType, MessageQueue, UnboundedMessageQueueSemantics }
import akka.event.LoggerMessageQueueSemantics
//<start id="mailbox_statistics"/>
case class MonitorEnvelope(queueSize: Int,
                           receiver: String,
                           entryTime: Long,
                           handle: Envelope) //<co id="ch14-mailbox-support-2" />

case class MailboxStatistics(queueSize: Int,
                             receiver: String,
                             sender: String,
                             entryTime: Long,
                             exitTime: Long) //<co id="ch14-mailbox-support-1" />
//<end id="mailbox_statistics"/>

//<start id="monitor_queue"/>
//<start id="monitor_queue_constructor"/>
class MonitorQueue(val system: ActorSystem)  //<co id="ch14-monitor_queue_sys" />
    extends MessageQueue //<co id="monitor_queue_mq" />
    with UnboundedMessageQueueSemantics  //<co id="unbounded_semantics" />
    with LoggerMessageQueueSemantics { //<co id="logger_semantics" />
  private final val queue = new ConcurrentLinkedQueue[MonitorEnvelope]() //<co id="conc_queue" />
//<end id="monitor_queue_constructor"/>

//<start id="the_rest"/>
  def numberOfMessages = queue.size //<co id="ch14-mailbox-support2-1" />
  def hasMessages = !queue.isEmpty //<co id="ch14-mailbox-support2-2" />

  def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = { //<co id="ch14-mailbox-support2-3" />
    if (hasMessages) {
      var envelope = dequeue
      while (envelope ne null) {
        deadLetters.enqueue(owner, envelope)
        envelope = dequeue
      }
    }
  }
//<end id="the_rest"/>

//<start id="enqueue"/>
  def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
    val env = MonitorEnvelope(queueSize = queue.size() + 1,
      receiver = receiver.toString(),
      entryTime = System.currentTimeMillis(),
      handle = handle)
    queue add env
  }
//<end id="enqueue"/>

//<start id="dequeue"/>
  def dequeue(): Envelope = {
    val monitor = queue.poll()
    if (monitor != null) {
      monitor.handle.message match {
        case stat: MailboxStatistics => //skip message <co id="ch14-mailbox-dequeue-1" />
        case _ => { //<co id="ch14-mailbox-dequeue-2" />
          val stat = MailboxStatistics(
            queueSize = monitor.queueSize,
            receiver = monitor.receiver,
            sender = monitor.handle.sender.toString(),
            entryTime = monitor.entryTime,
            exitTime = System.currentTimeMillis())
          system.eventStream.publish(stat)
        }
      }
      monitor.handle //<co id="ch14-mailbox-dequeue-3" />
    } else {
      null //<co id="ch14-mailbox-dequeue-4" />
    }
  }
//<end id="dequeue"/>

}
//<end id="monitor_queue"/>

//<start id="ch14-mailboxType"/>
class MonitorMailboxType(settings: ActorSystem.Settings, config: Config)
    extends MailboxType 
    with ProducesMessageQueue[MonitorQueue]{ //<co id="ch14-mailboxType-1" />

  final override def create(owner: Option[ActorRef],
                            system: Option[ActorSystem]): MessageQueue = {
    system match {
      case Some(sys) =>
        new MonitorQueue(sys) //<co id="ch14-mailboxType-2" />
      case _ =>
        throw new IllegalArgumentException("requires a system") //<co id="ch14-mailboxType-3" />
    }
  }
}
//<end id="ch14-mailboxType"/>
