package ch03

import akka.actor._

class LifeCycleHooks extends Actor with ActorLogging {
  println("Constructor")
  //<start id="ch3-life-start"/>
  override def preStart() {
    println("preStart") //<co id="ch3-life-start-1" />
  }
  //<end id="ch3-life-start"/>

  //<start id="ch3-life-stop"/>
  override def postStop() {
    println("postStop") //<co id="ch3-life-stop-1" />
  }
  //<end id="ch3-life-stop"/>

  //<start id="ch3-life-pre-restart"/>
  override def preRestart(reason: Throwable, //<co id="ch3-life-pre-restart-1a" />
                          message: Option[Any]) { //<co id="ch3-life-pre-restart-1b" />
    println("preRestart")
    super.preRestart(reason, message) //<co id="ch3-life-pre-restart-2" />
  }
  //<end id="ch3-life-pre-restart"/>

  //<start id="ch3-life-post-restart"/>
  override def postRestart(reason: Throwable) { //<co id="ch3-life-post-restart-1" />
    println("postRestart")
    super.postRestart(reason) //<co id="ch3-life-post-restart-2" />

  }
  //<end id="ch3-life-post-restart"/>

  def receive = {
    case "restart" =>
      throw new IllegalStateException("force restart")
    case msg: AnyRef =>
      println("Receive")
      sender ! msg
  }
}
