package com.goticks

import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import akka.util.Timeout
import scala.concurrent.duration._
import akka.cluster.routing.{ClusterRouterSettings}
import akka.routing.{BroadcastRouter}
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster
import spray.routing.RequestContext
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.routing.ClusterRouterConfig
import akka.routing.Broadcast
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.UnreachableMember
import com.goticks.TicketProtocol.{Event, Events}

class RestInterface extends HttpServiceActor
                    with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging with BoxOfficeCreator { actor: Actor =>
  import com.goticks.TicketProtocol._
  import context._
  implicit val timeout = Timeout(10 seconds)
  import akka.pattern.ask
  import akka.pattern.pipe

  val boxOffice = createBoxOffice

  val broadcast = context.actorOf(
    Props[BoxOffice].withRouter(
      ClusterRouterConfig(BroadcastRouter(),
        ClusterRouterSettings(10000,"/user/boxOffice",false,Some("boxOffice")))
    )
  )

  val clusterListener = system.actorOf(Props[ClusterListener],
    name = "clusterListener")
  Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])


  def routes: Route =
    path("clusterevents") {
      get { ctx =>
        clusterListener ! ctx
        boxOffice.tell(Broadcast(GetEvents),clusterListener)
      }
    } ~
    path("events") {
      put {
        entity(as[Event]) { event => requestContext =>
          log.info(s"Received new event $event, sending to $boxOffice")
          val responder = createResponder(requestContext)
          boxOffice.ask(event).pipeTo(responder)
        }
      } ~
      get { requestContext =>
        val responder = createResponder(requestContext)
        boxOffice.ask(GetEvents).pipeTo(responder)
      }
    } ~
    path("ticket") {
      get {
        entity(as[TicketRequest]) { ticketRequest => requestContext =>
          val responder = createResponder(requestContext)
          boxOffice.ask(ticketRequest).pipeTo(responder)
        }
      }
    } ~
    path("ticket" / Segment) { eventName =>
      get { ctx =>
        val req = TicketRequest(eventName)
        val responder = createResponder(ctx)
        boxOffice.ask(req).pipeTo(responder)
      }
    }

  def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext, boxOffice)))
  }

}

class Responder(requestContext:RequestContext, boxOffice:ActorRef) extends Actor with ActorLogging {
  import TicketProtocol._
  import spray.httpx.SprayJsonSupport._

  context.setReceiveTimeout(30 seconds)

  def receive = {

    case ticket:Ticket =>
      requestContext.complete(StatusCodes.OK, ticket)
      self ! PoisonPill

    case EventCreated =>
      requestContext.complete(StatusCodes.OK)
      self ! PoisonPill

    case SoldOut =>
      requestContext.complete(StatusCodes.NotFound)
      self ! PoisonPill

    case Events(events) =>
      requestContext.complete(StatusCodes.OK, events)
      self ! PoisonPill

    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)
      self ! PoisonPill

  }
}

class ClusterListener extends Actor with ActorLogging {
  import spray.httpx.SprayJsonSupport._
  var memberAddresses = Set[Address]()
  var nrOfMembers = 0
  var eventsReceived = 0
  var events = List[Event]()
  var ctx:Option[RequestContext] = None

  def receive = {
    case state: CurrentClusterState ⇒
      log.info("Current members: {}", state.members.mkString(", "))
      memberAddresses = state.members.filter(_.hasRole("boxOffice")).map(_.address).toSet

    case MemberUp(member) ⇒
      log.info("Member is Up: {}", member.address)
      if(member.hasRole("boxOffice")){
        memberAddresses = memberAddresses + member.address
        log.info(s"Member up, now ${memberAddresses.size} boxOffice members.")
      }
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: {}", member)
      if(member.hasRole("boxOffice")){
        memberAddresses = memberAddresses - member.address
        log.info(s"Unreachable node, ${memberAddresses.size} boxOffice members left.")
      }
    case MemberRemoved(member, _) ⇒
      log.info("Member is Removed: {}",
        member.address)
      if(member.hasRole("boxOffice")) {
        memberAddresses = memberAddresses - member.address

        log.info(s"Member removed, ${memberAddresses.size} boxOffice members left.")
      }
    case Events(receivedEvents) =>
      log.info(s"$eventsReceived events collected so far.")
       eventsReceived = eventsReceived + 1
       events = events ++ receivedEvents

       if(eventsReceived == memberAddresses.size) {
         log.info(s"Received all events from all boxOffice members $events")
         ctx.foreach(c=> c.complete(StatusCodes.OK,events))
         events = List()
         eventsReceived = 0
       }

    case newCtx: RequestContext =>
      eventsReceived = 0
      ctx = Some(newCtx)

    case _: ClusterDomainEvent ⇒ // ignore
  }
}
