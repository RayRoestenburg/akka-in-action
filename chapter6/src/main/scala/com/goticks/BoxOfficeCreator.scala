package com.goticks

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorIdentity
import akka.actor.Identify

trait BoxOfficeCreator { this: Actor =>
  def createBoxOffice:ActorRef = context.actorOf(Props[BoxOffice],"boxOffice")
}

object RemoteBoxOfficeCreator {
  val config = ConfigFactory.load("frontend").getConfig("backend")
  val host = config.getString("host")
  val port = config.getInt("port")
  val protocol = config.getString("protocol")
  val systemName = config.getString("system")
  val actorName = config.getString("actor")
}

trait RemoteBoxOfficeCreator extends BoxOfficeCreator { this:Actor =>
  import RemoteBoxOfficeCreator._

  def createPath:String = {
    s"$protocol://$systemName@$host:$port/$actorName"
  }

  override def createBoxOffice:ActorRef = {
    val path = createPath
    context.actorOf(Props(classOf[RemoteLookup],path), "lookupBoxOffice")
  }
}

import scala.concurrent.duration._

class RemoteLookup(path:String)
  extends Actor with ActorLogging {

  context.setReceiveTimeout(3 seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    val selection = context.actorSelection(path)
    selection ! Identify(path)
  }

  def receive = identify

  def identify:Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("switching to active state")
      context.become(active(actor))
      context.watch(actor)

    case ActorIdentity(`path`, None) =>
      log.error(s"Remote actor with path $path is not available.")

    case ReceiveTimeout =>
      sendIdentifyRequest()

    case msg:Any =>
      log.error(s"Ignoring message $msg, remote actor is not ready yet.")
  }

  def active(actor: ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info("Actor $actorRef terminated.")
      log.info("switching to identify state")
      context.become(identify)
      context.setReceiveTimeout(3 seconds)
      sendIdentifyRequest()

    case msg:Any => actor forward msg
  }
}

trait ConfiguredRemoteBoxOfficeDeployment extends BoxOfficeCreator { this:Actor =>

  override def createBoxOffice = {
    context.actorOf(Props[RemoteBoxOfficeForwarder],
                           "forwarder")
  }
}

class RemoteBoxOfficeForwarder extends Actor with ActorLogging {
  context.setReceiveTimeout(3 seconds)

  deployAndWatch()

  def deployAndWatch(): Unit = {
    val actor = context.actorOf(Props[BoxOffice], "boxOffice")
    context.watch(actor)
    log.info("switching to maybe active state")
    context.become(maybeActive(actor))
    context.setReceiveTimeout(Duration.Undefined)
  }

  def receive = deploying

  def deploying:Receive = {

    case ReceiveTimeout =>
      deployAndWatch()

    case msg:Any =>
      log.error(s"Ignoring message $msg, remote actor is not ready yet.")
  }

  def maybeActive(actor:ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info("Actor $actorRef terminated.")
      log.info("switching to deploying state")
      context.become(deploying)
      context.setReceiveTimeout(3 seconds)
      deployAndWatch()

    case msg:Any => actor forward msg
  }
}

trait RemoteBoxOfficeDeployment extends BoxOfficeCreator { this:Actor =>

  override def createBoxOffice = {
    val config = ConfigFactory.load("frontend").getConfig("backend")
    val host = config.getString("host")
    val port = config.getInt("port")
    val protocol = config.getString("protocol")
    val systemName = config.getString("system")
    val actorName = config.getString("actor")
    val path = s"$protocol://$systemName@$host:$port/$actorName"
    val lookup = context.system.actorOf(Props(classOf[RemoteLookup],path), "lookupBoxOffice")
    lookup
  }
}
