package aia.faulttolerance

import aia.faulttolerance.LifeCycleHooks.{ForceRestart, ForceRestartException}
import akka.actor._

object LifeCycleHooks {

  object SampleMessage

  object ForceRestart

  private class ForceRestartException extends IllegalArgumentException("force restart")

}

class LifeCycleHooks extends Actor with ActorLogging {
  log.info("Constructor")
  //<start id="ch3-life-start"/>
  override def preStart(): Unit = {
    log.info("preStart") //<co id="ch3-life-start-1" />
  }
  //<end id="ch3-life-start"/>

  //<start id="ch3-life-stop"/>
  override def postStop(): Unit = {
    log.info("postStop") //<co id="ch3-life-stop-1" />
  }
  //<end id="ch3-life-stop"/>

  //<start id="ch3-life-pre-restart"/>
  override def preRestart(reason: Throwable, //<co id="ch3-life-pre-restart-1a" />
                          message: Option[Any]): Unit = { //<co id="ch3-life-pre-restart-1b" />
    log.info(s"preRestart. Reason: $reason when handling message: $message")
    super.preRestart(reason, message) //<co id="ch3-life-pre-restart-2" />
  }
  //<end id="ch3-life-pre-restart"/>

  //<start id="ch3-life-post-restart"/>
  override def postRestart(reason: Throwable): Unit = { //<co id="ch3-life-post-restart-1" />
    log.info("postRestart")
    super.postRestart(reason) //<co id="ch3-life-post-restart-2" />

  }
  //<end id="ch3-life-post-restart"/>

  def receive = {
    case ForceRestart =>
      throw new ForceRestartException
    case msg: AnyRef =>
      log.info(s"Received: '$msg'. Sending back")
      sender() ! msg
  }
}
