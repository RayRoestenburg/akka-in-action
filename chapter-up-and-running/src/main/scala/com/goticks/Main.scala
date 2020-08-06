package com.goticks

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.util.Timeout
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success}

object Main extends App
    with RequestTimeout {

  val config = ConfigFactory.load() 
  val host = config.getString("http.host") // Gets the host and a port from the configuration
  val port = config.getInt("http.port")

  implicit val system = ActorSystem()  // ActorMaterializer requires an implicit ActorSystem
  implicit val ec = system.dispatcher  // bindingFuture.map requires an implicit ExecutionContext

  val api = new RestApi(system, requestTimeout(config)).routes // the RestApi provides a Route
 
  implicit val materializer = Materializer.matFromSystem  // bindAndHandle requires an implicit materializer
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) //Starts the HTTP server
 
  val log =  Logging(system.eventStream, "go-ticks")
  bindingFuture.onComplete(eachBinding => {
    eachBinding match {
      case Success(serverBinding) => {
        log.info(s"RestApi bound to ${serverBinding.localAddress} ")
      }
      case Failure(ex) =>
        log.error(ex, "Failed to bind to {}:{}!", host, port)
        // on failure, terminate the system gracefully
        system.terminate()
    }

  })
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
