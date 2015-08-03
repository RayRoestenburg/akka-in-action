package aia.routing

import akka.actor.{ Props, ActorRef, Actor }
import scala.collection.mutable.ListBuffer

//<start id="ch09-routing-slip-msg"/>
object CarOptions extends Enumeration {
  val CAR_COLOR_GRAY, NAVIGATION, PARKING_SENSORS = Value
}

case class Order(options: Seq[CarOptions.Value])
case class Car(color: String = "",
               hasNavigation: Boolean = false,
               hasParkingSensors: Boolean = false)
//<end id="ch09-routing-slip-msg"/>

//<start id="ch09-routing-slip-slip"/>
case class RouteSlipMessage(routeSlip: Seq[ActorRef], //<co id="ch09-routing-slip-slip-1" />
                            message: AnyRef)

trait RouteSlip {

  def sendMessageToNextTask(routeSlip: Seq[ActorRef],
                            message: AnyRef) {
    val nextTask = routeSlip.head //<co id="ch09-routing-slip-slip-2" />
    val newSlip = routeSlip.tail
    if (newSlip.isEmpty) { //<co id="ch09-routing-slip-slip-3" />
      nextTask ! message
    } else {

      nextTask ! RouteSlipMessage( //<co id="ch09-routing-slip-slip-4" />
        routeSlip = newSlip,
        message = message)
    }
  }
}
//<end id="ch09-routing-slip-slip"/>

//<start id="ch09-routing-slip-tasks"/>
class PaintCar(color: String) extends Actor with RouteSlip { //<co id="ch09-routing-slip-task-1" />
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(color = color))
    }
  }
}

class AddNavigation() extends Actor with RouteSlip { //<co id="ch09-routing-slip-task-2" />
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(hasNavigation = true))
    }
  }
}

class AddParkingSensors() extends Actor with RouteSlip {
  def receive = { //<co id="ch09-routing-slip-task-3" />
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(hasParkingSensors = true))
    }
  }
}
//<end id="ch09-routing-slip-tasks"/>

//<start id="ch09-routing-slip-router"/>
class SlipRouter(endStep: ActorRef) extends Actor with RouteSlip {
  val paintBlack = context.actorOf( //<co id="ch09-routing-slip-router-1" />
    Props(new PaintCar("black")), "paintBlack")
  val paintGray = context.actorOf( //<co id="ch09-routing-slip-router-2" />
    Props(new PaintCar("gray")), "paintGray")
  val addNavigation = context.actorOf( //<co id="ch09-routing-slip-router-3" />
    Props[AddNavigation], "navigation")
  val addParkingSensor = context.actorOf( //<co id="ch09-routing-slip-router-4" />
    Props[AddParkingSensors], "parkingSensors")

  def receive = {
    case order: Order => {
      val routeSlip = createRouteSlip(order.options) //<co id="ch09-routing-slip-router-5" />

      sendMessageToNextTask(routeSlip, new Car) //<co id="ch09-routing-slip-router-6" />
    }
  }

  private def createRouteSlip(options: Seq[CarOptions.Value]):
      Seq[ActorRef] = {

    val routeSlip = new ListBuffer[ActorRef]
    //car needs a color
    if (!options.contains(CarOptions.CAR_COLOR_GRAY)) {
      routeSlip += paintBlack
    }
    options.foreach { //<co id="ch09-routing-slip-router-7" />
      case CarOptions.CAR_COLOR_GRAY  => routeSlip += paintGray
      case CarOptions.NAVIGATION      => routeSlip += addNavigation
      case CarOptions.PARKING_SENSORS => routeSlip += addParkingSensor
      case other                      => //do nothing
    }
    routeSlip += endStep
    routeSlip
  }
}

//<end id="ch09-routing-slip-router"/>