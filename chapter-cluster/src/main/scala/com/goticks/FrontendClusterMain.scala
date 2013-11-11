package com.goticks

import com.typesafe.config.ConfigFactory
import akka.actor._
import spray.can.Http.Bind
import spray.can.Http
import akka.cluster.routing.{ClusterRouterSettings}
import akka.routing.{ConsistentHashingRouter}
import com.goticks.TicketProtocol.{TicketRequest, Event}
import akka.cluster.routing.ClusterRouterConfig
import scala.Some

object FrontendClusterMain extends App {

  if(args.size == 1) {
    System.setProperty("NETTY_PORT",args(0))
  }

  val config = ConfigFactory.load("frontend-cluster")

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val system = ActorSystem("goticks", config)

  class FrontendRestInterface extends RestInterface
  with ClusteredBoxOffice

  val restInterface = system.actorOf(Props[FrontendRestInterface],
    "restInterface")

  Http(system).manager ! Bind(listener = restInterface,
    interface = host,
    port =port)
}


trait ClusteredBoxOffice extends BoxOfficeCreator { this: Actor =>
  override def createBoxOffice:ActorRef = {
    import ConsistentHashingRouter._

    def hashMapping: ConsistentHashMapping = {
      case Event(event, _) ⇒ event
      case TicketRequest(event) => event
    }

    context.actorOf(
      Props[BoxOffice].withRouter(
        ClusterRouterConfig(ConsistentHashingRouter(hashMapping = hashMapping, nrOfInstances = 1000),
          ClusterRouterSettings(10000,"/user/boxOffice",false,Some("boxOffice")))
      )
    )
  }
}
