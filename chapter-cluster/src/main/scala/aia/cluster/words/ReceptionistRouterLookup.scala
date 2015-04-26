package aia.cluster
package words

import akka.actor._
import akka.cluster.routing._
import akka.routing._

trait ReceptionistRouterLookup { this: Actor =>
  def receptionistRouter = context.actorOf(
    ClusterRouterGroup(
      BroadcastGroup(Nil),
      ClusterRouterGroupSettings(
        totalInstances = 100,
        routeesPaths = List("/user/receptionist"),
        allowLocalRoutees = true,
        useRole = Some("master")
      )
    ).props(),
    name = "receptionist-router")
}
