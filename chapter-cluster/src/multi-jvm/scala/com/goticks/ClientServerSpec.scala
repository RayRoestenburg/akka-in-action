package com.goticks

import akka.remote.testkit.MultiNodeSpec

import akka.testkit.ImplicitSender
import akka.actor._
import TicketProtocol._

class ClientServerSpecMultiJvmFrontend extends ClientServerSpec
class ClientServerSpecMultiJvmBackend extends ClientServerSpec

class ClientServerSpec extends MultiNodeSpec(ClientServerConfig)
with STMultiNodeSpec with ImplicitSender {

  import ClientServerConfig._

  trait TestRemoteBoxOfficeCreator extends RemoteBoxOfficeCreator { this:Actor =>

    override def createPath: String = {
      val actorPath = node(backend) / "user" /"boxOffice"
      actorPath.toString
    }
  }

  def initialParticipants = roles.size

  "A Client Server configured app" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "be able to create an event and sell a ticket" in {

      runOn(frontend) {
        enterBarrier("deployed")
        val restInterface = system.actorOf(Props(new RestInterfaceMock with TestRemoteBoxOfficeCreator))

        val path = node(backend) / "user" / "boxOffice"
        val actorSelection = system.actorSelection(path)

        actorSelection.tell(Identify(path), testActor)

        val actorRef = expectMsgPF() {
          case ActorIdentity(`path`, ref) => ref
        }

        restInterface ! Event("RHCP", 1)

        expectMsg(EventCreated)

        restInterface ! TicketRequest("RHCP")

        expectMsg(Ticket("RHCP", 1))
      }

      runOn(backend) {
        system.actorOf(Props[BoxOffice], "boxOffice")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}

class RestInterfaceMock extends Actor with RemoteBoxOfficeCreator with ActorLogging {
  val boxOffice = createBoxOffice

  def receive = {
    case msg:Any =>
      boxOffice forward msg
  }
}

