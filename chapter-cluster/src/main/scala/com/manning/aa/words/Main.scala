package com.manning.aa.words

import akka.actor.{Actor, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import akka.cluster.Cluster
import com.manning.aa.words.JobReceptionist.JobRequest
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}

object Main extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("words", config)

  println(s"Starting node with roles: ${Cluster(system).selfRoles}")

  if(system.settings.config.getStringList("akka.cluster.roles").contains("master")) {
    println("Starting job receptionist on master node..")
    //TODO start spray with this receptionist
    val receptionist = system.actorOf(Props[JobReceptionist], "receptionist")

    //TODO remove
    Cluster(system).subscribe( system.actorOf(Props(new Actor() {
      def receive: Actor.Receive = {
        case MemberUp(member) =>
          if(member.address == Cluster(system).selfAddress) {
            println("Master is up!")
            val text = List("this is a test", "of some very naive word counting", "but what can you say", "it is what it is")

            receptionist ! JobRequest("the first job", (1 to 100000).flatMap(i => text ++ text).toList)
          }

        case s:CurrentClusterState =>
          println(s"cluster state $s")

      }
    })),classOf[MemberUp])
  }
}