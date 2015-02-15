package aia.performance.throughput

import akka.testkit.TestProbe
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import aia.performance.{ProcessCPURequest, SystemMessage, ProcessRequest}
import concurrent.duration._

class ThroughputTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/through")
  implicit val system = ActorSystem("ThroughputTest", configuration)

  "System" must {
    "fails to perform" in {
      val nrMessages = 99
      val nrWorkers = 3
      val statDuration = 2000 millis //((nrMessages * 10)+1000)/4 millis

      val end = TestProbe()
      val workers = system.actorOf(
        RoundRobinPool(nrWorkers).props(Props(new ProcessRequest(1 second, end.ref)).withDispatcher("my-dispatcher")),
        "Workers")

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
      /* default
total process time 51022 Average=515
(akka://ThroughputTest/user/Workers/$a,51022)
(akka://ThroughputTest/user/Workers/$c,48026)
(akka://ThroughputTest/user/Workers/$b,38022)

throughput 1
total process time 50022 Average=505
(akka://ThroughputTest/user/Workers/$a,46022)
(akka://ThroughputTest/user/Workers/$c,50022)
(akka://ThroughputTest/user/Workers/$b,49021)

throughput 10
total process time 53023 Average=535
(akka://ThroughputTest/user/Workers/$a,43067)
(akka://ThroughputTest/user/Workers/$c,53023)
(akka://ThroughputTest/user/Workers/$b,46067)
total process time 53030 Average=535
(akka://ThroughputTest/user/Workers/$a,33026)
(akka://ThroughputTest/user/Workers/$c,53030)
(akka://ThroughputTest/user/Workers/$b,46026)

throughput 33
total process time 66023 Average=666
(akka://ThroughputTest/user/Workers/$a,33024)
(akka://ThroughputTest/user/Workers/$c,66023)
(akka://ThroughputTest/user/Workers/$b,33022)

default 999 messages
total process time 501024 Average=501
(akka://ThroughputTest/user/Workers/$a,501024)
(akka://ThroughputTest/user/Workers/$c,498024)
(akka://ThroughputTest/user/Workers/$b,458024)
*/
    }
  }
}

