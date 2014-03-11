package com.goticks

import akka.actor._
import akka.persistence._
// ticketSeller keeps it's state
// a view can show the snap state
//  seller (tickets) -> (tickets -n)
//  Tickets => add tickets
//  BuyTicket => remove ticket.

// T,B,B,B,B,T,B,B,B,T => 23 tickets.

// can 'NewTickets' fail? no not really
// can BuyTicket fail? yes (but it does not effect the state). And where is the sender?
// ordering of buyticket and Tickets is important off course
// snapshot means apply to vector tickets.
class TicketProcessor extends Processor {
  import TicketProtocol._

  var tickets = Vector[Ticket]()
  override def processorId = self.path.name

  val channel = context.actorOf(Channel.props(), name = "myChannel")

  def receive = {
    case GetEvents =>
      sender ! tickets.size
      println("Tickets: "+tickets)
    case p @ Persistent(Tickets(newTickets), _) =>
      println("new tickets:" + p)
      tickets = tickets ++ newTickets
    case p @ Persistent(BuyTicket, _) =>
      println("buying ticket:" + p)
      if (tickets.isEmpty) {
        println("------------------->EMPTY<------------------")
        channel ! Deliver(p.withPayload(SoldOut), sender.path)
      }
      tickets.headOption.foreach { ticket =>
        tickets = tickets.tail
        channel ! Deliver(p.withPayload(ticket), sender.path)
      }
    case "boom" => throw new Exception("BOOM!")
  }
}

class TicketsView(theProcessorId: String) extends View {
  import TicketProtocol._
  override def processorId = theProcessorId
  // Define the view for the tickets of an event.
  def receive = {
    case Persistent(Tickets(newTickets), _) =>
      //println("VIEW: new tickets!")
    case Persistent(BuyTicket, _) =>
      //println("VIEW: ticket bought!")
  }
}


object TicketProtocol {
  case class Event(event:String, nrOfTickets:Int)
  case object GetEvents
  case class Events(events:List[Event])
  case object EventCreated
  case class TicketRequest(event:String)
  case object SoldOut
  case class Tickets(tickets:List[Ticket])
  case object BuyTicket
  case class Ticket(event:String, nr:Int)
}

class TicketPurchaser(ticketSeller: ActorRef) extends Actor {
  import TicketProtocol._

  def receive = {
    case p @ ConfirmablePersistent(payload, sequenceNr, redeliveries) =>
      payload match {
        case i: Int => println("received nr of tickets: "+i)
        case t : Ticket =>  println("received ticket:"+t)
        case SoldOut => println("received soldout")
      }
      p.confirm()
    case msg =>
      println("purchasin.")
      ticketSeller ! Persistent(msg)
  }
}

object TicketsExample extends App {
  import TicketProtocol._
  val system = ActorSystem("tickets")
  val processor = system.actorOf(Props[TicketProcessor], "RHCP")
  val purchaser = system.actorOf(Props(new TicketPurchaser(processor)), "purchaser")
  val view = system.actorOf(Props(new TicketsView(processor.path.name)), "RHCP-view")
  purchaser ! Tickets(List(Ticket("RHCP", 1), Ticket("RHCP", 2), Ticket("RHCP", 3)))
  purchaser ! BuyTicket
  processor ! Recover()
  Thread.sleep(1000)
  purchaser ! BuyTicket
  purchaser ! BuyTicket
  purchaser ! BuyTicket

  Thread.sleep(1000)
  system.shutdown()
}
