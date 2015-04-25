package com.goticks

// <start id="ch02-main-imports"/>
import akka.actor.{ ActorSystem , Actor, Props } // <co id="ch02_import_actor"/>
import akka.io.IO // <co id="ch02_import_io"/>
import akka.pattern.ask // <co id="ch02_import_ask"/>
import akka.util.Timeout // <co id="ch02_import_timeout"/>

import com.typesafe.config.{ Config, ConfigFactory } // <co id="ch02_import_config"/>

import spray.can.Http // <co id="ch02_import_http"/>

// <end id="ch02-main-imports"/>

// <start id="ch02-start-http"/>
object Main extends App
    with RequestTimeout
    with ShutdownIfNotBound {

  val config = ConfigFactory.load() // <co id="ch02_load_config"/>
  val host = config.getString("http.host") // <co id="ch02_get_http_pars"/>
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("goticks") // <co id="ch02_create_actorsystem"/>

  implicit val executionContext = system.dispatcher

  implicit val timeout = requestTimeout(config) // <co id="ch02_timeout_for_asking"/>

  val api = system.actorOf(Props(new RestApi(timeout)), "httpInterface") // <co id="ch02_toplevelactor"/>

  val response = IO(Http).ask(Http.Bind(listener = api, interface = host, port = port)) // <co id="ch02_startServer"/>
  shutdownIfNotBound(response) // <co id="ch02_http_server_bind_response"/>
}
// <end id="ch02-start-http"/>

// <start id="ch02-support-traits"/>
trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = { // <co id="ch02_timeout_spray_can"/>
    val t = config.getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

trait ShutdownIfNotBound {
  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  def shutdownIfNotBound(f: Future[Any]) // <co id="ch02_shutdownIfNotBound"/>
    (implicit system:ActorSystem, ec: ExecutionContext) = {
    f.mapTo[Http.Event].map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) => // <co id="http_command_failed"/>
        println(s"REST interface could not bind: ${cmd.failureMessage}, shutting down.")
        system.shutdown()
    }.recover {
      case e: Throwable =>
        println(s"Unexpected error binding to HTTP: ${e.getMessage}, shutting down.")
        system.shutdown()
    }
  }
}
// <end id="ch02-support-traits"/>
