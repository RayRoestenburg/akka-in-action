package aia.performance.dispatcher

import akka.testkit.TestProbe
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.routing.RoundRobinPool
import aia.performance.monitor.ActorStatistics
import com.typesafe.config.ConfigFactory
import aia.performance.{SystemMessage, ProcessRequest, PrintMsg}
import aia.performance.monitor.{MonitorActor, MailboxStatistics, ActorStatistics, MonitorStatisticsActor}
import concurrent.duration._


class DispatcherPinnedTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/dispatcher")
  implicit val system = ActorSystem("DispatcherTest", configuration)

  "System" must {
    "Workers fails to perform" in {
      //val nrMessages = 15000
      val nrMessages = 6000
      val nrWorkers = 100
      val statDuration = 15000 millis //((nrMessages * 10)+1000)/4 millis

      //      val statProbe = TestProbe()
      //      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
      //        storeSummaries = statProbe.ref)),"stat")
      val printer = system.actorOf(Props[PrintMsg], "Printer-workers")
      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
        storeSummaries = printer)).withDispatcher("my-pinned-dispatcher"), "stat-workers")
      system.eventStream.subscribe(
        stat,
        classOf[ActorStatistics])
      system.eventStream.subscribe(
        stat,
        classOf[MailboxStatistics])

      val end = TestProbe()
      val workers = system.actorOf(
        RoundRobinPool(nrWorkers).props(
          Props(new ProcessRequest(1 second, end.ref) with MonitorActor)),
        "Workers-workers")

      val firstStep = system.actorOf(Props(new ProcessRequest(10 millis, workers) with MonitorActor).withDispatcher("my-pinned-dispatcher"), "Entry")

      for (i <- 0 until nrMessages) {
        firstStep ! new SystemMessage()
        Thread.sleep(15)
      }
      Thread.sleep(10000)
      printer ! "print"
      //      val statMsg = statProbe.expectMsgAllOf[Any]()
      //      println(statMsg)
      val msg = end.receiveN(n = nrMessages, max = 30 seconds)

      system.stop(firstStep)
      system.stop(workers)
      system.stop(stat)
      system.stop(printer)
      /*
Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$G],1376236035000,1376236050000,10,0.0,33574.0,21.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$z],1376236035000,1376236050000,14,0.0,19902.0,28.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1376236035000,1376236050000,3,74.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$P],1376236035000,1376236050000,11,0.0,58427.0,5.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$J],1376236035000,1376236050000,9,0.0,41049.0,18.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$L],1376236035000,1376236050000,15,0.0,47485.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$Q],1376236035000,1376236050000,20,0.0,43690.0,61.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$K],1376236035000,1376236050000,15,0.0,40006.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$A],1376236035000,1376236050000,10,0.0,27453.0,28.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$U],1376236035000,1376236050000,5,0.0,67365.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$C],1376236035000,1376236050000,15,0.0,16144.0,48.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$M],1376236035000,1376236050000,21,0.0,24713.0,54.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$E],1376236035000,1376236050000,17,0.0,21897.0,78.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1376236035000,1376236050000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$R],1376236035000,1376236050000,5,0.0,54695.0,15.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$O],1376236035000,1376236050000,20,0.0,43956.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$H],1376236035000,1376236050000,18,0.0,38310.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$N],1376236035000,1376236050000,17,0.0,46801.0,60.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$F],1376236035000,1376236050000,7,0.0,17620.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236035000,1376236050000,1,66.0,0.0,66.0,10.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$y],1376236035000,1376236050000,8,0.0,26196.0,18.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$D],1376236035000,1376236050000,6,0.0,21092.0,5.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$T],1376236035000,1376236050000,6,0.0,65492.0,38.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$I],1376236035000,1376236050000,5,0.0,13051.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$S],1376236035000,1376236050000,8,0.0,64623.0,51.0,1000.0))
!!!!!!PRINT!!!!!!!! nr=15
ENTRY: maxQueue=2, utilization 65,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235975000,1376235990000,2,65.0,0.0,65.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235990000,1376236005000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236005000,1376236020000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236020000,1376236035000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236035000,1376236050000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236050000,1376236065000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236065000,1376236080000,1,63.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236080000,1376236095000,1,66.0,0.0,66.0,9.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236095000,1376236110000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236110000,1376236125000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236125000,1376236140000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236140000,1376236155000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236155000,1376236170000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236170000,1376236185000,1,66.0,0.0,66.0,10.0)
ENTRY: maxQueue=1, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376236185000,1376236200000,1,66.0,0.0,66.0,10.0)

StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$W],1376236200000,1376236215000,25,0.0,200305.0,33.0,1000.0),
StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$5],1376236200000,1376236215000,40,0.0,179679.0,33.0,1000.0),
StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$8],1376236200000,1376236215000,18,0.0,209638.0,47.0,1000.0),
StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$sb],1376236200000,1376236215000,5,0.0,230841.0,33.0,1000.0),

       */

    }
  }
}

