package aia.performance.throughput

import akka.testkit.TestProbe
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import aia.performance.{ProcessCPURequest, SystemMessage, ProcessRequest}
import concurrent.duration._


class ThroughputCPUTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/through")
  implicit val system = ActorSystem("ThroughputTest", configuration)

  "System" must {
    "fails to with cpu" in {
      val nrWorkers = 40
      val nrMessages = nrWorkers * 40

      val end = TestProbe()
      val workers = system.actorOf(
        RoundRobinPool(nrWorkers).props(
          Props(new ProcessCPURequest(250 millis, end.ref)).withDispatcher("my-dispatcher")),
        "Workers-cpu")

      val startTime = System.currentTimeMillis()
      for (i <- 0 until nrMessages) {
        workers ! new SystemMessage(startTime, 0, "")
      }
      val msg = end.receiveN(n = nrMessages, max = 9000 seconds).asInstanceOf[Seq[SystemMessage]]
      val endTime = System.currentTimeMillis()
      val total = endTime - startTime
      println("total process time %d Average=%d".format(total, total / nrMessages))
      val grouped = msg.groupBy(_.id)
      grouped.map {
        case (key, listMsg) => (key, listMsg.foldLeft(0L) { (m, x) => math.max(m, x.duration) })
      }.foreach(println(_))

      Thread.sleep(1000)

      system.stop(workers)
      /* throughput 5
total process time 104094 Average=65
      */
    }
  }
}

