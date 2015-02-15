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

class DispatcherThroughputTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/dispatcher")
  implicit val system = ActorSystem("DispatcherTest", configuration)

  "System" must {
    "fails to perform change throughput" in {
      //val nrMessages = 15000
      val nrMessages = 6000
      val nrWorkers = 100
      val statDuration = 15000 millis

      val printer = system.actorOf(Props[PrintMsg].withDispatcher("my-pinned-dispatcher"), "Printer-throughput")
      val stat = system.actorOf(Props(new MonitorStatisticsActor(period = statDuration, processMargin = 1000,
        storeSummaries = printer)).withDispatcher("my-pinned-dispatcher"), "stat-throughput")
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
            withDispatcher("throughput-dispatcher")),
        "Workers")

      val firstStep = system.actorOf(Props(new ProcessRequest(10 millis, workers) with MonitorActor).withDispatcher("throughput-dispatcher"), "Entry")

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
      !!!!!!PRINT!!!!!!!! nr=16
ENTRY: maxQueue=62, utilization 13,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347145000,1377347160000,62,13.0,444.0,13.0,10.0)
ENTRY: maxQueue=162, utilization 68,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347160000,1377347175000,162,68.0,1006.0,68.0,10.0)
ENTRY: maxQueue=22, utilization 2,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347175000,1377347190000,22,2.0,106.0,2.0,10.0)
ENTRY: maxQueue=1000, utilization 66,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347190000,1377347205000,1000,66.0,13638.0,66.0,10.0)
ENTRY: maxQueue=1076, utilization 49,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347250000,1377347265000,1076,49.0,65285.0,49.0,10.0)
ENTRY: maxQueue=1000, utilization 17,000000:  StatisticsSummary(Actor[akka://DispatcherTest/user/Entry],1377347265000,1377347280000,1000,17.0,62787.0,17.0,10.0)

Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$3],1377347295000,1377347310000,30,1.0,74768.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1377347295000,1377347310000,2,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$5],1377347295000,1377347310000,12,0.0,137622.0,76.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$2],1377347295000,1377347310000,31,1.0,70230.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$w],1377347295000,1377347310000,10,0.0,42402.0,62.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$4],1377347295000,1377347310000,19,1.0,122255.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$Y],1377347295000,1377347310000,33,0.0,32120.0,37.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$x],1377347295000,1377347310000,9,0.0,43363.0,56.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1377347295000,1377347310000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$0],1377347295000,1377347310000,33,0.0,35321.0,58.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$Z],1377347295000,1377347310000,33,0.0,33082.0,43.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$v],1377347295000,1377347310000,10,0.0,30429.0,23.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$y],1377347295000,1377347310000,7,0.0,45603.0,41.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1377347295000,1377347310000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$1],1377347295000,1377347310000,32,1.0,65047.0,100.0,1000.0))
Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$3],1377347310000,1377347325000,33,0.0,44896.0,22.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$z],1377347310000,1377347325000,10,0.0,52926.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1377347310000,1377347325000,2,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$5],1377347310000,1377347325000,27,1.0,105218.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$8],1377347310000,1377347325000,13,0.0,151399.0,82.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$2],1377347310000,1377347325000,33,0.0,44092.0,17.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$4],1377347310000,1377347325000,33,0.0,71917.0,96.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$6],1377347310000,1377347325000,15,1.0,146960.0,96.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$~],1377347310000,1377347325000,2,0.0,165312.0,10.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$A],1377347310000,1377347325000,10,0.0,55188.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$9],1377347310000,1377347325000,12,0.0,152782.0,74.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$+],1377347310000,1377347325000,4,0.0,162482.0,25.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$x],1377347310000,1377347325000,10,0.0,43361.0,10.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1377347310000,1377347325000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$B],1377347310000,1377347325000,1,0.0,66208.0,3.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$y],1377347310000,1377347325000,10,0.0,45600.0,25.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1377347310000,1377347325000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$7],1377347310000,1377347325000,14,0.0,149057.0,89.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$1],1377347310000,1377347325000,33,0.0,42644.0,7.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$w],1377347310000,1377347325000,0,0.0,0.0,3.0,0.0))
Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1377347325000,1377347340000,2,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$5],1377347325000,1377347340000,33,0.0,63070.0,43.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$8],1377347325000,1377347340000,28,1.0,114371.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$bb],1377347325000,1377347340000,6,0.0,174969.0,37.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$6],1377347325000,1377347340000,30,1.0,104912.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$~],1377347325000,1377347340000,17,1.0,155794.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$9],1377347325000,1377347340000,27,1.0,120379.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$+],1377347325000,1377347340000,19,1.0,151894.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1377347325000,1377347340000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$B],1377347325000,1377347340000,10,0.0,66206.0,62.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$ab],1377347325000,1377347340000,9,0.0,171353.0,56.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1377347325000,1377347340000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$7],1377347325000,1377347340000,29,1.0,109608.0,100.0,1000.0))
Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1377347340000,1377347355000,2,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$8],1377347340000,1377347355000,33,0.0,77032.0,37.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$bb],1377347340000,1377347355000,21,1.0,161612.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$6],1377347340000,1377347355000,33,0.0,75040.0,23.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$~],1377347340000,1377347355000,32,1.0,106487.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$9],1377347340000,1377347355000,33,0.0,78231.0,45.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$+],1377347340000,1377347355000,33,0.0,101557.0,94.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$C],1377347340000,1377347355000,10,0.0,85342.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$E],1377347340000,1377347355000,9,0.0,88542.0,54.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1377347340000,1377347355000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$cb],1377347340000,1377347355000,11,0.0,183703.0,69.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$ab],1377347340000,1377347355000,24,1.0,147769.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$F],1377347340000,1377347355000,2,0.0,95314.0,9.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$D],1377347340000,1377347355000,10,0.0,87344.0,62.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1377347340000,1377347355000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$7],1377347340000,1377347355000,33,0.0,76002.0,30.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$db],1377347340000,1377347355000,1,0.0,196001.0,5.0,1000.0))
Received List(StatisticsSummary(Actor[akka://DispatcherTest/user/stat],1377347355000,1377347370000,2,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$J],1377347355000,1377347370000,10,0.0,97284.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$bb],1377347355000,1377347370000,32,0.0,103918.0,76.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$fb],1377347355000,1377347370000,7,0.0,203872.0,42.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$jb],1377347355000,1377347370000,4,0.0,208038.0,21.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$gb],1377347355000,1377347370000,5,0.0,206204.0,30.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$eb],1377347355000,1377347370000,7,0.0,203862.0,42.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$K],1377347355000,1377347370000,10,0.0,98482.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$ib],1377347355000,1377347370000,4,0.0,207682.0,23.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$E],1377347355000,1377347370000,10,0.0,88540.0,11.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/system/testActor1],1377347355000,1377347370000,1,8.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$cb],1377347355000,1377347370000,26,1.0,151082.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$ab],1377347355000,1377347370000,32,0.0,96002.0,57.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$hb],1377347355000,1377347370000,5,0.0,206380.0,29.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$F],1377347355000,1377347370000,10,0.0,95311.0,57.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Printer],1377347355000,1377347370000,1,0.0,0.0,0.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$I],1377347355000,1377347370000,10,0.0,97108.0,66.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$db],1377347355000,1377347370000,16,1.0,187915.0,100.0,1000.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$~],1377347355000,1377347370000,0,0.0,0.0,2.0,0.0), StatisticsSummary(Actor[akka://DispatcherTest/user/Workers/$D],1377347355000,1377347370000,0,0.0,0.0,3.0,0.0))


      */
    }
  }
}

