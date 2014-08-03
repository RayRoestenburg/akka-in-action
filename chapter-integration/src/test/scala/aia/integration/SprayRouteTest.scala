package aia.integration

import org.scalatest.WordSpec
import spray.testkit.ScalatestRouteTest
import akka.actor.{ Props, ActorSystem }
import akka.testkit.TestProbe
import spray.routing.HttpService

/*
class SprayRouteTest extends ScalatestRouteTest
  with WordSpec with OrderService with MustMatchers {
  def actorRefFactory = system
  val orderSystem = system.actorOf(Props[ProcessOrders])

  "The service" must {
    "return a response for GET requests to the root path" in {
      Get() ~> myRoute ~> check {

        entityAs[String] must be ("Say hello")
      }
    }
  }
}


class SprayRouteTest extends ScalatestRouteTest
with WordSpec with HttpService {
  def actorRefFactory = system

  val myRoute = path("test") {
    get {
      complete("Say hello")
    }
  }

  "The service" must {
    "return a response for GET requests to the root path" in {
      Get() ~> myRoute ~> check {

        entityAs[String] must be ("Say hello")
      }
    }
  }
}
         */
/**
 * class SprayRouteTest extends ScalatestRouteTest
 * with WordSpec with HttpService {
 * def actorRefFactory = system
 *
 * val myRoute = path("test") {
 * get {
 * complete("Say hello")
 * }
 * }
 *
 * "The service" must {
 * "return a response for GET requests to the root path" in {
 * Get() ~> myRoute ~> check {
 *
 * entityAs[String] must be ("Say hello")
 * }
 * }
 * }
 * }
 *
 */
