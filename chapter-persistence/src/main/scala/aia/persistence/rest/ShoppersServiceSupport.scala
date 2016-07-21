package aia.persistence.rest

import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import aia.persistence._

trait ShoppersServiceSupport extends RequestTimeout {
  def startService(shoppers: ActorRef)(implicit system: ActorSystem) = {
    val config = system.settings.config
    val settings = Settings(system)
    val host = settings.http.host
    val port = settings.http.port

    implicit val ec = system.dispatcher  //bindAndHandle requires an implicit ExecutionContext

    val api = new ShoppersService(shoppers, system, requestTimeout(config)).routes // the RestApi provides a Route
 
    implicit val materializer = ActorMaterializer()
    val bindingFuture: Future[ServerBinding] =
      Http().bindAndHandle(api, host, port)
   
    val log =  Logging(system.eventStream, "shoppers")
    bindingFuture.map { serverBinding =>
      log.info(s"Shoppers API bound to ${serverBinding.localAddress} ")
    }.onFailure { 
      case ex: Exception =>
        log.error(ex, "Failed to bind to {}:{}!", host, port)
        system.terminate()
    }
  }
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = { //<co id="ch02_timeout_spray_can"/>
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
