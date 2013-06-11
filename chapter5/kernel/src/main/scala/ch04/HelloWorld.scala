package ch04

import akka.actor.{ActorRef, ActorLogging, Actor}
import scala.concurrent.duration._

//<start id="ch04-hello"/>
class HelloWorld extends Actor
  with ActorLogging {  //<co id="ch04-hello-log"/>

  def receive = {
    case msg:String  =>
      val hello = "Hello %s".format(msg)
      sender ! hello
      log.info("Sent response {}",hello)
  }
}
//<end id="ch04-hello"/>

//<start id="ch04-caller"/>
class HelloWorldCaller(timer:Duration, actor:ActorRef)
  extends Actor with ActorLogging {

  case class TimerTick(msg:String)

  override def preStart() {
    super.preStart()
    context.system.scheduler.schedule(   //<co id="ch04-schedule-1"/>
      timer,                             //<co id="ch04-schedule-2"/>
      timer,                             //<co id="ch04-schedule-3"/>
      self,                              //<co id="ch04-schedule-4"/>
      new TimerTick("everybody"))        //<co id="ch04-schedule-5"/>
  }

  def receive = {
    case msg: String  => log.info("received {}",msg)
    case tick: TimerTick => actor ! tick.msg
  }
}
//<end id="ch04-caller"/>