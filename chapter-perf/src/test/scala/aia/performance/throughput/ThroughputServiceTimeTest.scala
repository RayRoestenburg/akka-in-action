package aia.performance.throughput

import akka.testkit.TestProbe
import akka.actor.{Props, ActorSystem}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import aia.performance.{ProcessCPURequest, SystemMessage, ProcessRequest}
import concurrent.duration._

class ThroughputServiceTimeTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  val configuration = ConfigFactory.load("performance/through")
  implicit val system = ActorSystem("ThroughputTest", configuration)

  "System" must {
    "fails to with low service time" in {
      val nrWorkers = 40
      val nrMessages = nrWorkers * 40 * 1000

      val end = TestProbe()
      val workers = system.actorOf(
        RoundRobinPool(nrWorkers).props(
          Props(new ProcessRequest(0 millis, end.ref)).withDispatcher("my-dispatcher")),
        "Workers-service")

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
      /*
      troughput 1 average 43882
      troughput 2 average 42969
      troughput 3 average 39015
      troughput 5 average 41569
      troughput 8 average 37339
      troughput 10 average 37288
      troughput 20 average 37146


      val nrWorkers = 40
      val nrMessages = nrWorkers * 40 * 1000
          throughput 1
          total process time 48034 Average=0
          total process time 46783 Average=0
          total process time 44147 Average=0
          total process time 43562 Average=0
          total process time 40811 Average=0
          total process time 39959 Average=0         average 43882

total process time 43449 Average=0
(akka://ThroughputTest/user/Workers/$y,41476)
(akka://ThroughputTest/user/Workers/$j,41996)
(akka://ThroughputTest/user/Workers/$J,41408)
(akka://ThroughputTest/user/Workers/$s,42091)
(akka://ThroughputTest/user/Workers/$G,42031)
(akka://ThroughputTest/user/Workers/$N,43445)
(akka://ThroughputTest/user/Workers/$g,43391)
(akka://ThroughputTest/user/Workers/$d,42803)
(akka://ThroughputTest/user/Workers/$D,42064)
(akka://ThroughputTest/user/Workers/$K,42080)
(akka://ThroughputTest/user/Workers/$k,43411)
(akka://ThroughputTest/user/Workers/$z,42158)
(akka://ThroughputTest/user/Workers/$v,42801)
(akka://ThroughputTest/user/Workers/$l,42159)
(akka://ThroughputTest/user/Workers/$L,43440)
(akka://ThroughputTest/user/Workers/$C,42142)
(akka://ThroughputTest/user/Workers/$c,42095)
(akka://ThroughputTest/user/Workers/$r,42143)
(akka://ThroughputTest/user/Workers/$F,41994)
(akka://ThroughputTest/user/Workers/$u,43420)
(akka://ThroughputTest/user/Workers/$H,42750)
(akka://ThroughputTest/user/Workers/$h,42073)
(akka://ThroughputTest/user/Workers/$b,41340)
(akka://ThroughputTest/user/Workers/$q,42005)
(akka://ThroughputTest/user/Workers/$f,42798)
(akka://ThroughputTest/user/Workers/$B,42111)
(akka://ThroughputTest/user/Workers/$i,43433)
(akka://ThroughputTest/user/Workers/$o,42867)
(akka://ThroughputTest/user/Workers/$I,43402)
(akka://ThroughputTest/user/Workers/$e,42159)
(akka://ThroughputTest/user/Workers/$n,43409)
(akka://ThroughputTest/user/Workers/$E,42767)
(akka://ThroughputTest/user/Workers/$x,42814)
(akka://ThroughputTest/user/Workers/$t,42782)
(akka://ThroughputTest/user/Workers/$a,42804)
(akka://ThroughputTest/user/Workers/$A,42062)
(akka://ThroughputTest/user/Workers/$w,42841)
(akka://ThroughputTest/user/Workers/$m,43449)
(akka://ThroughputTest/user/Workers/$p,42869)
(akka://ThroughputTest/user/Workers/$M,42870)
          ..2
          total process time 44165 Average=0
          total process time 44207 Average=0
          total process time 43344 Average=0
          total process time 40160 Average=0
          ..3
          total process time 42486 Average=0
          total process time 39097 Average=0
          total process time 37410 Average=0
          total process time 37068 Average=0
          total process time 34694 Average=0
          total process time 33608 Average=0
          throughput 5
          total process time 47750 Average=0
          total process time 42519 Average=0
          total process time 41799 Average=0
          total process time 38397 Average=0
          total process time 37383 Average=0
          throughput 8
          total process time 40949 Average=0
          total process time 36345 Average=0
          total process time 36090 Average=0
          total process time 35975 Average=0
          total process time 31619 Average=0
          throughput 10
          total process time 40901 Average=0
          total process time 39134 Average=0
          total process time 36738 Average=0
          total process time 36312 Average=0
          total process time 33355 Average=0
          throughput 20
          total process time 42137 Average=0
          total process time 41187 Average=0
          total process time 39804 Average=0
          total process time 37397 Average=0
          total process time 35516 Average=0
          total process time 30264 Average=0

total process time 38713 Average=0
(akka://ThroughputTest/user/Workers/$y,38710)
(akka://ThroughputTest/user/Workers/$j,35906)
(akka://ThroughputTest/user/Workers/$J,37281)
(akka://ThroughputTest/user/Workers/$s,37256)
(akka://ThroughputTest/user/Workers/$G,38131)
(akka://ThroughputTest/user/Workers/$N,37179)
(akka://ThroughputTest/user/Workers/$g,38176)
(akka://ThroughputTest/user/Workers/$d,35835)
(akka://ThroughputTest/user/Workers/$D,37265)
(akka://ThroughputTest/user/Workers/$K,38708)
(akka://ThroughputTest/user/Workers/$k,38133)
(akka://ThroughputTest/user/Workers/$z,35166)
(akka://ThroughputTest/user/Workers/$v,35887)
(akka://ThroughputTest/user/Workers/$l,37196)
(akka://ThroughputTest/user/Workers/$L,37316)
(akka://ThroughputTest/user/Workers/$C,37211)
(akka://ThroughputTest/user/Workers/$c,35822)
(akka://ThroughputTest/user/Workers/$r,38155)
(akka://ThroughputTest/user/Workers/$F,37286)
(akka://ThroughputTest/user/Workers/$u,35160)
(akka://ThroughputTest/user/Workers/$H,38713)
(akka://ThroughputTest/user/Workers/$h,37306)
(akka://ThroughputTest/user/Workers/$b,38129)
(akka://ThroughputTest/user/Workers/$q,38068)
(akka://ThroughputTest/user/Workers/$f,36505)
(akka://ThroughputTest/user/Workers/$B,38106)
(akka://ThroughputTest/user/Workers/$i,38121)
(akka://ThroughputTest/user/Workers/$o,37275)
(akka://ThroughputTest/user/Workers/$I,37281)
(akka://ThroughputTest/user/Workers/$e,37284)
(akka://ThroughputTest/user/Workers/$n,38151)
(akka://ThroughputTest/user/Workers/$E,38194)
(akka://ThroughputTest/user/Workers/$x,35821)
(akka://ThroughputTest/user/Workers/$t,37348)
(akka://ThroughputTest/user/Workers/$a,36655)
(akka://ThroughputTest/user/Workers/$A,37310)
(akka://ThroughputTest/user/Workers/$w,38133)
(akka://ThroughputTest/user/Workers/$m,37344)
(akka://ThroughputTest/user/Workers/$p,37226)
(akka://ThroughputTest/user/Workers/$M,37223)
total process time 36394 Average=0
(akka://ThroughputTest/user/Workers/$y,35150)
(akka://ThroughputTest/user/Workers/$j,35119)
(akka://ThroughputTest/user/Workers/$J,35782)
(akka://ThroughputTest/user/Workers/$s,34282)
(akka://ThroughputTest/user/Workers/$G,35136)
(akka://ThroughputTest/user/Workers/$N,32861)
(akka://ThroughputTest/user/Workers/$g,33654)
(akka://ThroughputTest/user/Workers/$d,35835)
(akka://ThroughputTest/user/Workers/$D,35721)
(akka://ThroughputTest/user/Workers/$K,34324)
(akka://ThroughputTest/user/Workers/$k,35110)
(akka://ThroughputTest/user/Workers/$z,32816)
(akka://ThroughputTest/user/Workers/$v,35789)
(akka://ThroughputTest/user/Workers/$l,35807)
(akka://ThroughputTest/user/Workers/$L,33655)
(akka://ThroughputTest/user/Workers/$C,34287)
(akka://ThroughputTest/user/Workers/$c,35841)
(akka://ThroughputTest/user/Workers/$r,35865)
(akka://ThroughputTest/user/Workers/$F,35082)
(akka://ThroughputTest/user/Workers/$u,33562)
(akka://ThroughputTest/user/Workers/$H,34307)
(akka://ThroughputTest/user/Workers/$h,33629)
(akka://ThroughputTest/user/Workers/$b,35826)
(akka://ThroughputTest/user/Workers/$q,35030)
(akka://ThroughputTest/user/Workers/$f,36376)
(akka://ThroughputTest/user/Workers/$B,35142)
(akka://ThroughputTest/user/Workers/$i,35167)
(akka://ThroughputTest/user/Workers/$o,35846)
(akka://ThroughputTest/user/Workers/$I,33506)
(akka://ThroughputTest/user/Workers/$e,35140)
(akka://ThroughputTest/user/Workers/$n,35018)
(akka://ThroughputTest/user/Workers/$E,35730)
(akka://ThroughputTest/user/Workers/$x,35749)
(akka://ThroughputTest/user/Workers/$t,35801)
(akka://ThroughputTest/user/Workers/$a,33662)
(akka://ThroughputTest/user/Workers/$A,35763)
(akka://ThroughputTest/user/Workers/$w,35855)
(akka://ThroughputTest/user/Workers/$m,33640)
(akka://ThroughputTest/user/Workers/$p,35683)
(akka://ThroughputTest/user/Workers/$M,36393)



      val nrWorkers = 80
      val nrMessages = nrWorkers * 20 * 1000

          throughput 1
          total process time 46795 Average=0
          total process time 44927 Average=0
          total process time 43873 Average=0
          throughput 5
          total process time 35958 Average=0
          total process time 33041 Average=0
          throughput 10
          total process time 40645 Average=0
          total process time 40116 Average=0
      */
    }
  }
}

