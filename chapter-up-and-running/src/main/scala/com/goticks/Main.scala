package com.goticks

//<start id="ch02-main-imports"/>
import scala.concurrent.Future

import akka.actor.{ ActorSystem , Actor, Props } //<co id="ch02_import_actor"/>
import akka.event.Logging
import akka.util.Timeout //<co id="ch02_import_timeout"/>

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.{ Config, ConfigFactory } //<co id="ch02_import_config"/>

//<end id="ch02-main-imports"/>

//<start id="ch02-start-http"/>
object Main extends App
    with RequestTimeout {

  val config = ConfigFactory.load() 
  val host = config.getString("http.host") // Gets the host and a port from the configuration
  val port = config.getInt("http.port")

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher  //bindAndHandle requires an implicit ExecutionContext

  val api = new RestApi(system, requestTimeout(config)).routes // the RestApi provides a Route
 
  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) //Starts the HTTP server
 
  val log =  Logging(system.eventStream, "go-ticks")
  bindingFuture.map { serverBinding =>
    log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onFailure { 
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}

//<end id="ch02-start-http"/>

//<start id="ch02-support-traits"/>
trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = { //<co id="ch02_timeout_spray_can"/>
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

//<end id="ch02-support-traits"/>
