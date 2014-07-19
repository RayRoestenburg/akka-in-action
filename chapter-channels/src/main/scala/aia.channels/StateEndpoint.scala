package aia.channels

import akka.actor.Actor
import java.util.Date

case class StateEvent(time: Date, state: String)
case class Connection(time: Date, connected: Boolean)

class StateEndpoint extends Actor {
  def receive = {
    case Connection(time, true) => {
      context.system.eventStream.publish(new StateEvent(time, "Connected"))
    }
    case Connection(time, false) => {
      context.system.eventStream.publish(new StateEvent(time, "Disconnected"))
    }
  }
}

class SystemLog extends Actor {
  def receive = {
    case event: StateEvent => {
    }
  }
}

class SystemMonitor extends Actor {
  def receive = {
    case event: StateEvent => {
    }
  }
}

//<start id="ch09-eventBus"/>
import akka.event.ActorEventBus
import akka.event.{ LookupClassification, EventBus }

class OrderMessageBus extends EventBus //<co id="ch09-eventBus-1"/>
  with LookupClassification
  with ActorEventBus {

  type Event = Order //<co id="ch09-eventBus-2"/>
  type Classifier = Boolean
  def mapSize = 2

  protected def classify(event: OrderMessageBus#Event) = { //<co id="ch09-eventBus-3"/>
    event.number > 1
  }

  protected def publish(event: OrderMessageBus#Event, //<co id="ch09-eventBus-4"/>
                        subscriber: OrderMessageBus#Subscriber) {
    subscriber ! event
  }
}
//<end id="ch09-eventBus"/>

class MyEventBus extends EventBus with LookupClassification
  with ActorEventBus {

  type Event = AnyRef
  def mapSize = 2
  type Classifier = String

  protected def classify(event: MyEventBus#Event) = {
    "TestBus"
  }

  protected def publish(event: MyEventBus#Event,
                        subscriber: MyEventBus#Subscriber) {
    subscriber ! event
  }

  def subscribe(subscriber: Subscriber): Boolean =
    subscribers.put("TestBus", subscriber)
}
