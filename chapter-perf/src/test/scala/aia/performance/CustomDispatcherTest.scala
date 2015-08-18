package aia.performance

import akka.testkit.TestProbe
import akka.actor.{ Props, Actor, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import com.typesafe.config.ConfigFactory

class CustomDispatcherTest extends WordSpecLike with MustMatchers with BeforeAndAfterAll {
  val configuration = ConfigFactory.load("performance/pipelineDispatcher")
  implicit val system = ActorSystem("DispatcherTest", configuration)

  override def afterAll() {
    system.shutdown
    super.afterAll()
  }

  "Dispatcher" must {
    "createnew" in {
      val testActor = system.actorOf(Props(new TestActor(nrChild = 20)).withDispatcher("pipeline-dispatcher"), "root")
      val probe = TestProbe()
      probe.send(testActor, "test")
      probe.expectMsg("test")
      system.stop(testActor)
    }
  }
}

class TestActor(nrChild: Int) extends Actor {
  override def preStart() {
    for (nr <- 0 until nrChild) {
      context.actorOf(Props(new TestActor(nrChild = nrChild - 1)), "test" + nr)
    }
  }
  def receive = {
    case msg: AnyRef => sender() ! msg
  }
}

