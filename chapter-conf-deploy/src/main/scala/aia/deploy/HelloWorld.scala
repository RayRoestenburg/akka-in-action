package aia.deploy

import akka.actor.{ActorRef, ActorLogging, Actor}
import scala.concurrent.duration._

//<start id="ch07-hello"/>
class HelloWorld extends Actor
  with ActorLogging {  //<co id="ch07-hello-log"/>

  def receive = {
    case msg: String  =>
      val hello = "Hello %s".format(msg)
      sender() ! hello
      log.info("Sent response {}",hello)
  }
}
//<end id="ch07-hello"/>

//<start id="ch07-caller"/>
class HelloWorldCaller(timer: FiniteDuration, actor: ActorRef)
  extends Actor with ActorLogging {

  case class TimerTick(msg: String)

  override def preStart() {
    super.preStart()
    implicit val ec = context.dispatcher
    context.system.scheduler.schedule(   //<co id="ch07-schedule-1"/>
      timer,                             //<co id="ch07-schedule-2"/>
      timer,                             //<co id="ch07-schedule-3"/>
      self,                              //<co id="ch07-schedule-4"/>
      new TimerTick("everybody"))        //<co id="ch07-schedule-5"/>
  }

  def receive = {
    case msg: String  => log.info("received {}",msg)
    case tick: TimerTick => actor ! tick.msg
  }
}
//<end id="ch07-caller"/>
