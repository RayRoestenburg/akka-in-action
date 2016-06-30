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

class DispatcherInitTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/dispatcher")
  implicit val system = ActorSystem("DispatcherTest", configuration)

  "System" must {
    "fails to perform" in {
      //val nrMessages = 15000
      val nrMessages = 6000
      val nrWorkers = 100
      val statDuration = 15000 millis //((nrMessages * 10)+1000)/4 millis

      //      val statProbe = TestProbe()
      //      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
      //        storeSummaries = statProbe.ref)),"stat")
      val printer = system.actorOf(Props[PrintMsg].withDispatcher("my-pinned-dispatcher"), "Printer-fail")
      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
        storeSummaries = printer)).withDispatcher("my-pinned-dispatcher"), "stat-fail")
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
        "Workers-fail")

      val firstStep = system.actorOf(Props(new ProcessRequest(10 millis, workers) with MonitorActor), "Entry")

      for (i <- 0 until nrMessages) {
        firstStep ! new SystemMessage()
        Thread.sleep(15)
      }
      Thread.sleep(10000)
      printer ! "print"
      //      val statMsg = statProbe.expectMsgAllOf[Any]()
      //      println(statMsg)
      end.receiveN(n = nrMessages, max = 30 seconds)

      system.stop(firstStep)
      system.stop(workers)
      system.stop(stat)
      system.stop(printer)
      /*
!!!!!!PRINT!!!!!!!! nr=15
ENTRY: maxQueue=70, utilization 5,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235660000,1376235675000,70,5.0,4240.0,5.0,10.0)
ENTRY: maxQueue=179, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235675000,1376235690000,179,8.0,15774.0,8.0,10.0)
ENTRY: maxQueue=285, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235690000,1376235705000,285,8.0,29124.0,8.0,10.0)
ENTRY: maxQueue=385, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235705000,1376235720000,385,7.0,42193.0,7.0,10.0)
ENTRY: maxQueue=490, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235720000,1376235735000,490,8.0,55268.0,8.0,10.0)
ENTRY: maxQueue=595, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235735000,1376235750000,595,8.0,68617.0,8.0,10.0)
ENTRY: maxQueue=705, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235750000,1376235765000,705,8.0,81967.0,8.0,10.0)
ENTRY: maxQueue=810, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235765000,1376235780000,810,8.0,95317.0,8.0,10.0)
ENTRY: maxQueue=915, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235780000,1376235795000,915,8.0,108667.0,8.0,10.0)
ENTRY: maxQueue=1013, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235795000,1376235810000,1013,7.0,121913.0,7.0,10.0)
ENTRY: maxQueue=1121, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235810000,1376235825000,1121,7.0,134752.0,7.0,10.0)
ENTRY: maxQueue=1225, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235825000,1376235840000,1225,7.0,147656.0,7.0,10.0)
ENTRY: maxQueue=1325, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235840000,1376235855000,1325,8.0,160955.0,8.0,10.0)
ENTRY: maxQueue=1430, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235855000,1376235870000,1430,8.0,174305.0,8.0,10.0)
ENTRY: maxQueue=1540, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376235870000,1376235885000,1540,8.0,187655.0,8.0,10.0)


ENTRY: maxQueue=95, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233425000,1376233440000,95,7.0,5421.0,7.0,10.0)
ENTRY: maxQueue=200, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233440000,1376233455000,200,8.0,18492.0,8.0,10.0)
ENTRY: maxQueue=310, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233455000,1376233470000,310,8.0,32444.0,8.0,10.0)
ENTRY: maxQueue=425, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233470000,1376233485000,425,8.0,46394.0,8.0,10.0)
ENTRY: maxQueue=535, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233485000,1376233500000,535,8.0,60342.0,8.0,10.0)
ENTRY: maxQueue=645, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233500000,1376233515000,645,8.0,74292.0,8.0,10.0)
ENTRY: maxQueue=749, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233515000,1376233530000,749,7.0,87953.0,7.0,10.0)
ENTRY: maxQueue=860, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233530000,1376233545000,860,8.0,101611.0,8.0,10.0)
ENTRY: maxQueue=970, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233545000,1376233560000,970,8.0,115562.0,8.0,10.0)
ENTRY: maxQueue=1080, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233560000,1376233575000,1080,8.0,129513.0,8.0,10.0)
ENTRY: maxQueue=1195, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233575000,1376233590000,1195,8.0,143464.0,8.0,10.0)
ENTRY: maxQueue=1302, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233590000,1376233605000,1302,8.0,157415.0,8.0,10.0)
ENTRY: maxQueue=1405, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233605000,1376233620000,1405,7.0,171090.0,7.0,10.0)
ENTRY: maxQueue=1510, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233620000,1376233635000,1510,7.0,184437.0,7.0,10.0)
ENTRY: maxQueue=1625, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233635000,1376233650000,1625,8.0,198105.0,8.0,10.0)
ENTRY: maxQueue=1735, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233650000,1376233665000,1735,8.0,212055.0,8.0,10.0)
ENTRY: maxQueue=1850, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233665000,1376233680000,1850,8.0,226005.0,8.0,10.0)
ENTRY: maxQueue=1955, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233680000,1376233695000,1955,8.0,239954.0,8.0,10.0)
ENTRY: maxQueue=2065, utilization 8,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233695000,1376233710000,2065,8.0,253905.0,8.0,10.0)
ENTRY: maxQueue=2170, utilization 7,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1376233710000,1376233725000,2170,7.0,267571.0,7.0,10.0)
StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$wb],1376233425000,1376233440000,1,0.0,33.0,6.0,1000.0)
*/
    }
  }
}

