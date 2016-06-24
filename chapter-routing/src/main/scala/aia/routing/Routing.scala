package aia.routing

import scala.concurrent.duration._
import scala.collection.immutable

import akka.actor._
import akka.dispatch.Dispatchers
import akka.routing._

//<start id="ch09-routing-perf-msg"/>
case class PerformanceRoutingMessage(photo: String,
                                     license: Option[String],
                                     processedBy: Option[String]) //<co id="ch09-routing-perf-1" />
//<end id="ch09-routing-perf-msg"/>

case class SetService(id: String, serviceTime: FiniteDuration)

class GetLicense(pipe: ActorRef, initialServiceTime: FiniteDuration = 0 millis)
  extends Actor {
  var id = self.path.name
  var serviceTime = initialServiceTime

  def receive = {
    case init: SetService => {
      id = init.id
      serviceTime = init.serviceTime
      Thread.sleep(100)
    }
    case msg: PerformanceRoutingMessage => {
      Thread.sleep(serviceTime.toMillis)
      pipe ! msg.copy(
        license = ImageProcessing.getLicense(msg.photo),
        processedBy = Some(id) //<co id="ch09-routing-perf-2" />
        )
    }
  }
}

//<start id="ch09-routing-msgact"/>
class RedirectActor(pipe: ActorRef) extends Actor {
  println("RedirectActor instance created")
  def receive = {
    case msg: AnyRef => {
      pipe ! msg
    }
  }
}
//<end id="ch09-routing-msgact"/>

//<start id="ch09-routing-msg"/>
class SpeedRouterLogic(minSpeed: Int, normalFlowPath: String, cleanUpPath: String)
  extends RoutingLogic { //<co id="ch09-routing-msg-1" />

  def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee = {

    message match {
      case msg: Photo => //<co id="ch09-routing-msg-6" />
        if (msg.speed > minSpeed)
          findRoutee(routees, normalFlowPath) //<co id="ch09-routing-msg-7" />
        else
          findRoutee(routees, cleanUpPath) //<co id="ch09-routing-msg-8" />
    }
  }

  def findRoutee(routees: immutable.IndexedSeq[Routee], path: String): Routee = {
    val routeeList = routees.flatMap {
      case routee: ActorRefRoutee    => routees
      case SeveralRoutees(routeeSeq) => routeeSeq
    }
    val search = routeeList.find { case routee: ActorRefRoutee => routee.ref.path.toString().endsWith(path) }
    search.getOrElse(NoRoutee)
  }
}

case class SpeedRouterPool(minSpeed: Int, normalFlow: Props, cleanUp: Props) extends Pool {

  def nrOfInstances(sys: ActorSystem): Int = 1
  def resizer: Option[Resizer] = None
  def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()(SupervisorStrategy.defaultDecider)

  override def createRouter(system: ActorSystem): Router = {
    new Router(new SpeedRouterLogic(minSpeed, "normalFlow", "cleanup"))
  }

  override val routerDispatcher: String = Dispatchers.DefaultDispatcherId

  override def newRoutee(routeeProps: Props, context: ActorContext): Routee = {
    val normal = context.actorOf(normalFlow, "normalFlow")
    val clean = context.actorOf(cleanUp, "cleanup")

    SeveralRoutees(immutable.IndexedSeq[Routee](ActorRefRoutee(normal), ActorRefRoutee(clean)))
  }
}
//<end id="ch09-routing-msg"/>

//<start id="ch09-routing-state"/>
case class RouteStateOn()
case class RouteStateOff()

class SwitchRouter(normalFlow: ActorRef, cleanUp: ActorRef)
  extends Actor with ActorLogging {

  def on: Receive = { //<co id="ch09-routing-state-1" />
    case RouteStateOn =>
      log.warning("Received on while already in on state")
    case RouteStateOff => context.become(off) //<co id="ch09-routing-state-2" />
    case msg: AnyRef => {
      normalFlow ! msg //<co id="ch09-routing-state-3" />
    }
  }
  def off: Receive = { //<co id="ch09-routing-state-4" />
    case RouteStateOn => context.become(on) //<co id="ch09-routing-state-5" />
    case RouteStateOff =>
      log.warning("Received off while already in off state")
    case msg: AnyRef => {
      cleanUp ! msg //<co id="ch09-routing-state-6" />
    }
  }
  def receive = {
    case msg: AnyRef => off(msg) //<co id="ch09-routing-state-7" />
  }
}
//<end id="ch09-routing-state"/>

//<start id="ch09-routing-state2"/>

class SwitchRouter2(normalFlow: ActorRef, cleanUp: ActorRef)
  extends Actor with ActorLogging {

  def on: Receive = {
    case RouteStateOn =>
      log.warning("Received on while already in on state")
    case RouteStateOff => context.unbecome() //<co id="ch09-routing-state2-1" />
    case msg: AnyRef => {
      normalFlow ! msg
    }
  }
  def off: Receive = {
    case RouteStateOn => context.become(on)
    case RouteStateOff =>
      log.warning("Received off while already in off state")
    case msg: AnyRef => {
      cleanUp ! msg
    }
  }
  def receive = {
    case msg: AnyRef => off(msg)
  }
}
//<end id="ch09-routing-state2"/>
