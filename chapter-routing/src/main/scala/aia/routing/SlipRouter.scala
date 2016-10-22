package aia.routing

import akka.actor.{ Props, ActorRef, Actor }
import scala.collection.mutable.ListBuffer


object CarOptions extends Enumeration {
  val CAR_COLOR_GRAY, NAVIGATION, PARKING_SENSORS = Value
}

case class Order(options: Seq[CarOptions.Value])
case class Car(color: String = "",
               hasNavigation: Boolean = false,
               hasParkingSensors: Boolean = false)



case class RouteSlipMessage(routeSlip: Seq[ActorRef],
                            message: AnyRef)

trait RouteSlip {

  def sendMessageToNextTask(routeSlip: Seq[ActorRef],
                            message: AnyRef) {
    val nextTask = routeSlip.head
    val newSlip = routeSlip.tail
    if (newSlip.isEmpty) {
      nextTask ! message
    } else {

      nextTask ! RouteSlipMessage(
        routeSlip = newSlip,
        message = message)
    }
  }
}



class PaintCar(color: String) extends Actor with RouteSlip {
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(color = color))
    }
  }
}

class AddNavigation() extends Actor with RouteSlip {
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(hasNavigation = true))
    }
  }
}

class AddParkingSensors() extends Actor with RouteSlip {
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip,
        car.copy(hasParkingSensors = true))
    }
  }
}



class SlipRouter(endStep: ActorRef) extends Actor with RouteSlip {
  val paintBlack = context.actorOf(
    Props(new PaintCar("black")), "paintBlack")
  val paintGray = context.actorOf(
    Props(new PaintCar("gray")), "paintGray")
  val addNavigation = context.actorOf(
    Props[AddNavigation], "navigation")
  val addParkingSensor = context.actorOf(
    Props[AddParkingSensors], "parkingSensors")

  def receive = {
    case order: Order => {
      val routeSlip = createRouteSlip(order.options)

      sendMessageToNextTask(routeSlip, new Car)
    }
  }

  private def createRouteSlip(options: Seq[CarOptions.Value]):
      Seq[ActorRef] = {

    val routeSlip = new ListBuffer[ActorRef]
    //car needs a color
    if (!options.contains(CarOptions.CAR_COLOR_GRAY)) {
      routeSlip += paintBlack
    }
    options.foreach {
      case CarOptions.CAR_COLOR_GRAY  => routeSlip += paintGray
      case CarOptions.NAVIGATION      => routeSlip += addNavigation
      case CarOptions.PARKING_SENSORS => routeSlip += addParkingSensor
      case other                      => //do nothing
    }
    routeSlip += endStep
    routeSlip
  }
}

