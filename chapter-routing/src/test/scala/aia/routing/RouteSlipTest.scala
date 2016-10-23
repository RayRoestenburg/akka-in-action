package aia.routing

import akka.actor._
import org.scalatest._
import akka.testkit._

class RouteSlipTest
  extends TestKit(ActorSystem("RouteSlipTest"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Router" must {
    "route messages correctly" in {

      val probe = TestProbe()
      val router = system.actorOf(
        Props(new SlipRouter(probe.ref)), "SlipRouter")

      val minimalOrder = new Order(Seq())
      router ! minimalOrder
      val defaultCar = new Car(
        color = "black",
        hasNavigation = false,
        hasParkingSensors = false)
      probe.expectMsg(defaultCar)



      val fullOrder = new Order(Seq(
        CarOptions.CAR_COLOR_GRAY,
        CarOptions.NAVIGATION,
        CarOptions.PARKING_SENSORS))
      router ! fullOrder
      val carWithAllOptions = new Car(
        color = "gray",
        hasNavigation = true,
        hasParkingSensors = true)
      probe.expectMsg(carWithAllOptions)


      val msg = new Order(Seq(CarOptions.PARKING_SENSORS))
      router ! msg
      val expectedCar = new Car(
        color = "black",
        hasNavigation = false,
        hasParkingSensors = true)
      probe.expectMsg(expectedCar)

    }
  }
}