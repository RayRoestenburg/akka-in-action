package aia.structure

import scala.concurrent.duration._

import akka.actor._

import org.scalatest._
import akka.testkit._
import scala.language.postfixOps

class PipeAndFilterTest
  extends TestKit(ActorSystem("PipeAndFilterTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The pipe and filter" must {
    "filter messages in configuration 1" in {
      //<start id="ch7-pipe-test1"/>
      val endProbe = TestProbe()
      val speedFilterRef = system.actorOf( //<co id="ch07-pipe-test-1" />
        Props(new SpeedFilter(50, endProbe.ref)))
      val licenseFilterRef = system.actorOf(
        Props(new LicenseFilter(speedFilterRef)))
      val msg = new Photo("123xyz", 60) //<co id="ch07-pipe-test-2" />
      licenseFilterRef ! msg
      endProbe.expectMsg(msg)

      licenseFilterRef ! new Photo("", 60) //<co id="ch07-pipe-test-3" />
      endProbe.expectNoMsg(1 second)

      licenseFilterRef ! new Photo("123xyz", 49) //<co id="ch07-pipe-test-4" />
      endProbe.expectNoMsg(1 second)
      //<end id="ch7-pipe-test1"/>
    }
    "filter messages in configuration 2" in {
      //<start id="ch7-pipe-test2"/>
      val endProbe = TestProbe()
      val licenseFilterRef = system.actorOf( //<co id="ch07-pipe-test-5" />
        Props(new LicenseFilter(endProbe.ref)))
      val speedFilterRef = system.actorOf(
        Props(new SpeedFilter(50, licenseFilterRef)))
      val msg = new Photo("123xyz", 60)
      speedFilterRef ! msg
      endProbe.expectMsg(msg)

      speedFilterRef ! new Photo("", 60)
      endProbe.expectNoMsg(1 second)

      speedFilterRef ! new Photo("123xyz", 49)
      endProbe.expectNoMsg(1 second)
      //<end id="ch7-pipe-test2"/>
    }
  }
}