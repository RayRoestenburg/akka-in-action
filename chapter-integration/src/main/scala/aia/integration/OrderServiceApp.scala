package aia.integration

import scala.concurrent.Future

import akka.actor.{ ActorSystem , Actor, Props }
import akka.event.Logging
import akka.util.Timeout

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.{ Config, ConfigFactory } 
//<start id="order_service_app"/>
object OrderServiceApp extends App
    with RequestTimeout { //<co id="request_timeout"/>
  val config = ConfigFactory.load() 
  val host = config.getString("http.host")
  val port = config.getInt("http.port") //<co id="from_config"/>

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher

  val processOrders = system.actorOf(
    Props(new ProcessOrders), "process-orders"
  ) //<co id="process_orders"/>

  val api = new OrderServiceApi(system, 
    requestTimeout(config), 
    processOrders).routes //<co id="routes"/>
 
  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) //<co id="bind_routes"/>
 
  val log =  Logging(system.eventStream, "order-service") //<co id="logging"/>
  bindingFuture.map { serverBinding =>
    log.info(s"Bound to ${serverBinding.localAddress} ") //<co id="success"/>
  }.onFailure { 
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate() //<co id="failed_to_bind"/>
  }
}
//<end id="order_service_app"/>

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
