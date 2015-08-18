package aia.state

import akka.actor.{ ActorRef, Actor, FSM }
import math.min
import scala.concurrent.duration._

// events
case class BookRequest(context: AnyRef, target: ActorRef)
case class BookSupply(nrBooks: Int)
case object BookSupplySoldOut
case object Done
case object PendingRequests

//responses
case object PublisherRequest
case class BookReply(context: AnyRef, reserveId: Either[String, Int])

//states
sealed trait State
case object WaitForRequests extends State
case object ProcessRequest extends State
case object WaitForPublisher extends State
case object SoldOut extends State
case object ProcessSoldOut extends State

case class StateData(nrBooksInStore: Int,
                     pendingRequests: Seq[BookRequest])

class Inventory(publisher: ActorRef) extends Actor
  with FSM[State, StateData] {

  var reserveId = 0
  startWith(WaitForRequests, new StateData(0, Seq()))

  when(WaitForRequests) {
    case Event(request: BookRequest, data: StateData) => {
      val newStateData = data.copy(
        pendingRequests = data.pendingRequests :+ request)
      if (newStateData.nrBooksInStore > 0) {
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForPublisher) using newStateData
      }
    }
    case Event(PendingRequests, data: StateData) => {
      if (data.pendingRequests.isEmpty) {
        stay
      } else if (data.nrBooksInStore > 0) {
        goto(ProcessRequest)
      } else {
        goto(WaitForPublisher)
      }
    }
  }
  when(WaitForPublisher) {
    case Event(supply: BookSupply, data: StateData) => {
      goto(ProcessRequest) using data.copy(
        nrBooksInStore = supply.nrBooks)
    }
    case Event(BookSupplySoldOut, _) => {
      goto(ProcessSoldOut)
    }
  }
  when(ProcessRequest) {
    case Event(Done, data: StateData) => {
      goto(WaitForRequests) using data.copy(
        nrBooksInStore = data.nrBooksInStore - 1,
        pendingRequests = data.pendingRequests.tail)
    }
  }
  when(SoldOut) {
    case Event(request: BookRequest, data: StateData) => {
      goto(ProcessSoldOut) using new StateData(0, Seq(request))
    }
  }
  when(ProcessSoldOut) {
    case Event(Done, data: StateData) => {
      goto(SoldOut) using new StateData(0, Seq())
    }
  }
  whenUnhandled {
    // common code for all states
    case Event(request: BookRequest, data: StateData) => {
      stay using data.copy(
        pendingRequests = data.pendingRequests :+ request)
    }
    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}",
        e, stateName, s)
      stay
    }
  }
  initialize

  onTransition {
    case _ -> WaitForRequests => {
      if (!nextStateData.pendingRequests.isEmpty) {
        // go to next state
        self ! PendingRequests
      }
    }
    case _ -> WaitForPublisher => {
      //send request to publisher
      publisher ! PublisherRequest
    }
    case _ -> ProcessRequest => {
      val request = nextStateData.pendingRequests.head
      reserveId += 1
      request.target !
        new BookReply(request.context, Right(reserveId))
      self ! Done
    }
    case _ -> ProcessSoldOut => {
      nextStateData.pendingRequests.foreach(request => {
        request.target !
          new BookReply(request.context, Left("SoldOut"))
      })
      self ! Done
    }
  }
}

//<start id="ch10-fsm-Publisher"/>
class Publisher(totalNrBooks: Int, nrBooksPerRequest: Int)
  extends Actor {

  var nrLeft = totalNrBooks
  def receive = {
    case PublisherRequest => {
      if (nrLeft == 0)
        sender() ! BookSupplySoldOut //<co id="ch10-fsm-Publisher-1"/>
      else {
        val supply = min(nrBooksPerRequest, nrLeft)
        nrLeft -= supply
        sender() ! new BookSupply(supply) //<co id="ch10-fsm-Publisher-2"/>
      }
    }
  }
}
//<end id="ch10-fsm-Publisher"/>

class InventoryWithTimer(publisher: ActorRef) extends Actor
  with FSM[State, StateData] {

  var reserveId = 0
  startWith(WaitForRequests, new StateData(0, Seq()))

  when(WaitForRequests) {
    case Event(request: BookRequest, data: StateData) => {
      val newStateData = data.copy(
        pendingRequests = data.pendingRequests :+ request)
      if (newStateData.nrBooksInStore > 0) {
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForPublisher) using newStateData
      }
    }
    case Event(PendingRequests, data: StateData) => {
      if (data.pendingRequests.isEmpty) {
        stay
      } else if (data.nrBooksInStore > 0) {
        goto(ProcessRequest)
      } else {
        goto(WaitForPublisher)
      }
    }
  }
  when(WaitForPublisher, stateTimeout = 5 seconds) {
    case Event(supply: BookSupply, data: StateData) => {
      goto(ProcessRequest) using data.copy(
        nrBooksInStore = supply.nrBooks)
    }
    case Event(BookSupplySoldOut, _) => {
      goto(ProcessSoldOut)
    }
    case Event(StateTimeout, _) => goto(WaitForRequests)
  }
  when(ProcessRequest) {
    case Event(Done, data: StateData) => {
      goto(WaitForRequests) using data.copy(
        nrBooksInStore = data.nrBooksInStore - 1,
        pendingRequests = data.pendingRequests.tail)
    }
  }
  when(SoldOut) {
    case Event(request: BookRequest, data: StateData) => {
      goto(ProcessSoldOut) using new StateData(0, Seq(request))
    }
  }
  when(ProcessSoldOut) {
    case Event(Done, data: StateData) => {
      goto(SoldOut) using new StateData(0, Seq())
    }
  }
  whenUnhandled {
    // common code for all states
    case Event(request: BookRequest, data: StateData) => {
      stay using data.copy(
        pendingRequests = data.pendingRequests :+ request)
    }
    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}",
        e, stateName, s)
      stay
    }
  }
  initialize

  onTransition {
    case _ -> WaitForRequests => {
      if (!nextStateData.pendingRequests.isEmpty) {
        // go to next state
        self ! PendingRequests
      }
    }
    case _ -> WaitForPublisher => {
      //send request to publisher
      publisher ! PublisherRequest
    }
    case _ -> ProcessRequest => {
      val request = nextStateData.pendingRequests.head
      reserveId += 1
      request.target !
        new BookReply(request.context, Right(reserveId))
      self ! Done
    }
    case _ -> ProcessSoldOut => {
      nextStateData.pendingRequests.foreach(request => {
        request.target !
          new BookReply(request.context, Left("SoldOut"))
      })
      self ! Done
    }
  }
}
