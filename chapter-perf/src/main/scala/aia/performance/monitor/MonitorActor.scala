package aia.performance.monitor

import akka.actor.Actor

case class ActorStatistics(receiver: String,
                           sender: String,
                           entryTime: Long,
                           exitTime: Long)

//<start id="ch14-MonitorActor"/>
trait MonitorActor extends Actor {

  abstract override def receive = {
    case m: Any => {
      val start = System.currentTimeMillis()
      super.receive(m) //<co id="ch14-MonitorActor-1" />
      val end = System.currentTimeMillis()

      val stat = ActorStatistics( //<co id="ch14-MonitorActor-2" />
        self.toString(),
        sender.toString(),
        start,
        end)
      context.system.eventStream.publish(stat)
    }
  }
}

//<end id="ch14-MonitorActor"/>