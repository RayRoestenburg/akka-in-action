package ch03

import akka.actor._
import akka.actor.Terminated

object DbStrategy2 {
  //<start id="ch03-termination"/>
  class DbWatcher(dbWriter: ActorRef) extends Actor with ActorLogging {
    context.watch(dbWriter) //<co id="ch03-termination-watch"/>
    def receive = {
      case Terminated(actorRef) => //<co id="ch03-termination-actorref"/>
        log.warning("Actor {} terminated", actorRef) //<co id="ch03-termination-log"/>
    }
  }
  //<end id="ch03-termination"/>
}