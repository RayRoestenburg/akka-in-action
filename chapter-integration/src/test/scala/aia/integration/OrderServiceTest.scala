package aia.integration

import scala.concurrent.duration._
import scala.xml.NodeSeq
import akka.actor.Props

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._ //<co id="xml_support"/>
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest

import org.scalatest.{ Matchers, WordSpec }
 
class OrderServiceTest extends WordSpec 
    with Matchers 
    with OrderService //<co id="order_service"/>
    with ScalatestRouteTest { //<co id="scalatestRouteTest"/>

  implicit val executionContext = system.dispatcher //<co id="executionContext"/>
  implicit val requestTimeout = akka.util.Timeout(1 second) //<co id="timeout"/>
  val processOrders = 
    system.actorOf(Props(new ProcessOrders), "orders")

  "The order service" should {
    "return NotFound if the order cannot be found" in {
      Get("/orders/1") ~> routes ~> check { //<co id="Get_check"/>
        status shouldEqual StatusCodes.NotFound
      }
    }

    "return the tracking order for an order that was posted" in {
      val xmlOrder = 
      <order><customerId>customer1</customerId>
        <productId>Akka in action</productId>
        <number>10</number>
      </order>
      
      Post("/orders", xmlOrder) ~> routes ~> check { //<co id="Post_check"/>
        status shouldEqual StatusCodes.OK
        val xml = responseAs[NodeSeq]
        val id = (xml \\ "id").text.toInt
        val orderStatus = (xml \\ "status").text
        id shouldEqual 1
        orderStatus shouldEqual "received"
      }
      Get("/orders/1") ~> routes ~> check { //<co id="Get_after_Post_check"/>
        status shouldEqual StatusCodes.OK
        val xml = responseAs[NodeSeq]
        val id = (xml \\ "id").text.toInt
        val orderStatus = (xml \\ "status").text
        id shouldEqual 1
        orderStatus shouldEqual "processing"
      }
    }
  }
} 