package aia.faulttolerance

import akka.actor._

class LifeCycleHooks extends Actor with ActorLogging {
  println("Constructor")

  override def preStart(): Unit = {
    println("preStart")
  }



  override def postStop(): Unit = {
    println("postStop")
  }



  override def preRestart(reason: Throwable,
                          message: Option[Any]): Unit = {
    println("preRestart")
    super.preRestart(reason, message)
  }



  override def postRestart(reason: Throwable): Unit = {
    println("postRestart")
    super.postRestart(reason)

  }


  def receive = {
    case "restart" =>
      throw new IllegalStateException("force restart")
    case msg: AnyRef =>
      println("Receive")
      sender() ! msg
  }
}
