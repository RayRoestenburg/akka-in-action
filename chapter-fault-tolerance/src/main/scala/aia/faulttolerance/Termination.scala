package aia.faulttolerance

import akka.actor._
import akka.actor.Terminated

object DbStrategy2 {

  class DbWatcher(dbWriter: ActorRef) extends Actor with ActorLogging {
    context.watch(dbWriter)
    def receive = {
      case Terminated(actorRef) =>
        log.warning("Actor {} terminated", actorRef)
    }
  }

}