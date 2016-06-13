package aia.routing

import akka.actor._
import org.scalatest._
import akka.testkit._

class RouteSlipTest
  extends TestKit(ActorSystem("RouteSlipTest"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll() {
    system.terminate()
  }

  "The Router" must {
    "route messages correctly" in {
      //<start id="ch09-routing-slip-test1"/>
      val probe = TestProbe()
      val router = system.actorOf( //<co id="ch09-routing-slip-test1-1" />
        Props(new SlipRouter(probe.ref)), "SlipRouter")

      val minimalOrder = new Order(Seq())
      router ! minimalOrder //<co id="ch09-routing-slip-test1-2" />
      val defaultCar = new Car(
        color = "black",
        hasNavigation = false,
        hasParkingSensors = false)
      probe.expectMsg(defaultCar) //<co id="ch09-routing-slip-test1-3" />
      //<end id="ch09-routing-slip-test1"/>

      //<start id="ch09-routing-slip-test2"/>
      val fullOrder = new Order(Seq(
        CarOptions.CAR_COLOR_GRAY,
        CarOptions.NAVIGATION,
        CarOptions.PARKING_SENSORS))
      router ! fullOrder //<co id="ch09-routing-slip-test2-1" />
      val carWithAllOptions = new Car(
        color = "gray",
        hasNavigation = true,
        hasParkingSensors = true)
      probe.expectMsg(carWithAllOptions) //<co id="ch09-routing-slip-test2-2" />
      //<end id="ch09-routing-slip-test2"/>

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