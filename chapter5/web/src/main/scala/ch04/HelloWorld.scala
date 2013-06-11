package ch04

import akka.actor.{ActorRef, ActorLogging, Actor}
import play.api.mvc.{Action, AsyncResult}
import play.api.mvc.Results._
import play.api.libs.concurrent._


import akka.actor.{Props, ActorSystem, Actor}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.play.mini.{Path, GET, Application}
import play.api.data.Form
import play.api.data.Forms._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import parallel.Future
import akka.event.Logging

class HelloWorld extends Actor
with ActorLogging {

  def receive = {
    case msg: String =>
      val hello = "Hello %s".format(msg)
      sender ! hello
      log.info("Send response {}", hello)
  }
}



//<start id="ch04-web-hello"/>
object PlayMiniHello extends Application {   //<co id="ch04-web-hello-0"/>
  lazy val system = ActorSystem("webhello")  //<co id="ch04-web-hello-1"/>
  lazy val actor = system.actorOf(Props[HelloWorld])
  implicit val timeout = Timeout(1000 milliseconds)
  val log = Logging(system,PlayMiniHello.getClass)

  def route = {
    case GET(Path("/test")) => Action {  //<co id="ch04-web-hello-2"/>
      Ok("TEST @ %s\n".format(System.currentTimeMillis))
    }

    case GET(Path("/hello")) => Action {  //<co id="ch04-web-hello-3"/>
      implicit request =>           //<co id="ch04-web-hello-3b"/>

      val name = try {
        writeForm.bindFromRequest.get  //<co id="ch04-web-hello-4"/>
      } catch {
        case ex:Exception => {
          log.warning("no name specified")
          system.settings.config.getString("helloWorld.name")
        }
      }
      AsyncResult {                    //<co id="ch04-web-hello-5"/>
        val resultFuture = actor ? name recover {  //<co id="ch04-web-hello-6"/>
          case ex:AskTimeoutException => "Timeout"  //<co id="ch04-web-hello-6a"/>
          case ex:Exception => {               //<co id="ch04-web-hello-6b"/>
            log.error("recover from "+ex.getMessage)
            "Exception:" + ex.getMessage
          }
        }
        val promise = resultFuture.asPromise   //<co id="ch04-web-hello-7"/>
        promise.map {                          //<co id="ch04-web-hello-8"/>
          case res:String => {
            log.info("result "+res)
            Ok(res)
          }
          case  ex:Exception => {
            log.error("Exception "+ex.getMessage)
            Ok(ex.getMessage)
          }
          case _ => {
            Ok("Unexpected message")
          }
        }
      }
    }
  }
  val writeForm = Form("name" -> text(1,10))  //<co id="ch04-web-hello-9"/>
}

//<end id="ch04-web-hello"/>
