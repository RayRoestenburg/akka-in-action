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

class DispatcherThreadPoolTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/dispatcher")
  implicit val system = ActorSystem("DispatcherTest", configuration)

  "System" must {
    "fail to perform threadPool" in {
      //val nrMessages = 15000
      val nrMessages = 6000
      val nrWorkers = 100
      val statDuration = 15000 millis

      val printer = system.actorOf(Props[PrintMsg], "Printer-threadpool")
      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
        storeSummaries = printer)), "stat-threadpool")
      system.eventStream.subscribe(
        stat,
        classOf[ActorStatistics])
      system.eventStream.subscribe(
        stat,
        classOf[MailboxStatistics])

      val end = TestProbe()

      val workers = system.actorOf(
        RoundRobinPool(nrWorkers).props(
          Props(new ProcessRequest(1 second, end.ref) with MonitorActor).
            withDispatcher("my-thread-dispatcher")),
        "Workers")

      val firstStep = system.actorOf(Props(new ProcessRequest(10 millis, workers) with MonitorActor), "Entry")

      for (i <- 0 until nrMessages) {
        firstStep ! new SystemMessage()
        Thread.sleep(15)
      }
      Thread.sleep(10000)
      printer ! "print"
      val msg = end.receiveN(n = nrMessages, max = 30 seconds)

      system.stop(firstStep)
      system.stop(workers)
      system.stop(stat)
      system.stop(printer)
      /*

Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$G],1378984500000,1378984515000,15,0.0,174146.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$r],1378984500000,1378984515000,15,0.0,163621.0,14.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$u],1378984500000,1378984515000,15,0.0,169265.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$z],1378984500000,1378984515000,15,0.0,169252.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$m],1378984500000,1378984515000,15,0.0,168136.0,13.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1378984500000,1378984515000,5,74.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$P],1378984500000,1378984515000,13,0.0,179513.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$J],1378984500000,1378984515000,15,0.0,174114.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$n],1378984500000,1378984515000,15,0.0,168128.0,13.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$s],1378984500000,1378984515000,15,0.0,163606.0,14.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$o],1378984500000,1378984515000,15,0.0,168124.0,13.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$w],1378984500000,1378984515000,15,0.0,169243.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$L],1378984500000,1378984515000,13,0.0,179529.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$Q],1378984500000,1378984515000,13,0.0,179498.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$K],1378984500000,1378984515000,13,0.0,179526.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$A],1378984500000,1378984515000,15,0.0,169237.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$p],1378984500000,1378984515000,15,0.0,168121.0,13.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$t],1378984500000,1378984515000,15,0.0,163603.0,14.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$x],1378984500000,1378984515000,15,0.0,169250.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$C],1378984500000,1378984515000,15,0.0,174145.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$M],1378984500000,1378984515000,13,0.0,179504.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$E],1378984500000,1378984515000,15,0.0,174133.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1378984500000,1378984515000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$R],1378984500000,1378984515000,13,0.0,179495.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$O],1378984500000,1378984515000,13,0.0,179527.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$B],1378984500000,1378984515000,15,0.0,169234.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$H],1378984500000,1378984515000,15,0.0,174132.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$N],1378984500000,1378984515000,13,0.0,179511.0,19.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$F],1378984500000,1378984515000,15,0.0,174130.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$q],1378984500000,1378984515000,15,0.0,163635.0,14.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1378984500000,1378984515000,1,66.0,0.0,66.0,10.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$v],1378984500000,1378984515000,15,0.0,169268.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$y],1378984500000,1378984515000,15,0.0,169266.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$D],1378984500000,1378984515000,15,0.0,174138.0,33.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1378984500000,1378984515000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$I],1378984500000,1378984515000,15,0.0,174117.0,33.0,1000.0))

      */
    }
  }
}

