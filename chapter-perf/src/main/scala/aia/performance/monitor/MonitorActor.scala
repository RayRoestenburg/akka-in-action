package aia.performance.monitor

import akka.actor.Actor

case class ActorStatistics(receiver: String,
                           sender: String,
                           entryTime: Long,
                           exitTime: Long)


trait MonitorActor extends Actor {

  abstract override def receive = {
    case m: Any => {
      val start = System.currentTimeMillis()
      super.receive(m)
      val end = System.currentTimeMillis()

      val stat = ActorStatistics(
        self.toString(),
        sender.toString(),
        start,
        end)
      context.system.eventStream.publish(stat)
    }
  }
}

