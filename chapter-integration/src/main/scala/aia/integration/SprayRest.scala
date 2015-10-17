package aia.integration

import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import spray.routing._
import spray.http._
import MediaTypes._
import xml.{ Elem, XML }
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._
import spray.can.Http
import akka.io.IO

// we don't implement our route structure directly in the service
// actor because we want to be able to test it independently,
// without having to spin up an actor
//<start id="ch08-rest-spray-serviceActor"/>
class OrderServiceActor(val orderSystem: ActorRef) extends Actor
    with OrderService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}
//<end id="ch08-rest-spray-serviceActor"/>

// this trait defines our service behavior independently from
// the service actor
//<start id="ch08-rest-spray-service"/>
trait OrderService extends HttpService {
  val orderSystem: ActorRef

  // we use the enclosing ActorContext's or ActorSystem's
  // dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  implicit val timeout: Timeout = 1 second

  val myRoute = path("orderTest") { //<co id="ch08-rest-spray-service-1"/>
    get { //<co id="ch08-rest-spray-service-2"/>
      parameters('id.as[Long]).as(OrderId) { orderId => //<co id="ch08-rest-spray-service-3"/>
        //get status
        complete { //<co id="ch08-rest-spray-service-4"/>
          val askFuture = orderSystem ? orderId
          askFuture.map { //<co id="ch08-rest-spray-service-5"/>
            case result: TrackingOrder => {
              <statusResponse>
                <id>{ result.id }</id>
                <status>{ result.status }</status>
              </statusResponse>
            }
            case result: NoSuchOrder => {
              <statusResponse>
                <id>{ result.id }</id>
                <status>ID is unknown</status>
              </statusResponse>
            }
          }
        }
      }
    } ~ //<co id="ch08-rest-spray-service-10"/>
      post { //<co id="ch08-rest-spray-service-6"/>
        //add order
        entity(as[String]) { body => //<co id="ch08-rest-spray-service-7"/>
          val order = XMLConverter.createOrder(body.toString)
          complete { //<co id="ch08-rest-spray-service-8"/>
            val askFuture = orderSystem ? order
            askFuture.map { //<co id="ch08-rest-spray-service-9"/>
              case result: TrackingOrder => {
                <confirm>
                  <id>{ result.id }</id>
                  <status>{ result.status }</status>
                </confirm>
              }
              case result: Any => {
                <confirm>
                  <status>
                    Response is unknown{ result.toString() }
                  </status>
                </confirm>
              }
            }
          }
        }
      }
  }
}
//<end id="ch08-rest-spray-service"/>

object XMLConverter {
  def createOrder(content: String): Order = {
    val xml = XML.loadString(content)
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }

}

//<start id="ch08-rest-spray-boot"/>
class OrderHttpServer(host: String, portNr: Int, orderSystem: ActorRef)
  extends App {

  val system = ActorSystem("OrderHttpServer")
  //create and start our service actor
  val service = system.actorOf(Props( //<co id="ch08-rest-spray-boot-1"/>
    new OrderServiceActor(orderSystem)), "my-service")

  //create a new HttpServer using our handler tell it where to bind to
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)
  val httpServer = IO(Http)(system) //<co id="ch08-rest-spray-boot-2"/>
  httpServer.ask(Http.Bind(listener = service,
    interface = host, port = portNr)) //<co id="ch08-rest-spray-boot-3"/>
    .mapTo[Http.Event]
    .map {
    case Http.Bound(address) =>
      println(s"REST interface bound to $address")
    case Http.CommandFailed(cmd) =>
      println("REST interface could not bind to " +
        s"$host:$portNr, ${cmd.failureMessage}")
      system.shutdown()
  }

  def stop() { //<co id="ch08-rest-spray-boot-4"/>
    httpServer ! Http.ClosedAll
    system.stop(httpServer)
    system.shutdown()
  }

}
//<end id="ch08-rest-spray-boot"/>
