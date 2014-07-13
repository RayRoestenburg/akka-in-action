package aia.cluster.words

import akka.actor.{ActorLogging, Actor}

import akka.cluster.{MemberStatus, Cluster}
import akka.cluster.ClusterEvent._

class ClusterDomainEventListener extends Actor
                    with ActorLogging {
  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive ={
    case MemberUp(member) =>
      log.info(s"$member UP.")
    case MemberExited(member)=>
      log.info(s"$member EXITED.")
    case MemberRemoved(member, previousState)=>
      if(previousState == MemberStatus.Exiting) {
        log.info(s"Member $member Previously gracefully exited, REMOVED.")
      } else {
        log.info(s"$member Previously downed after unreachable, REMOVED.")
      }
    case UnreachableMember(member) =>
      log.info(s"$member UNREACHABLE")
    case ReachableMember(member) =>
      log.info(s"$member REACHABLE")
    case state: CurrentClusterState =>
      log.info(s"Current state of the cluster: $state")
  }
  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
    super.postStop()
  }
}
