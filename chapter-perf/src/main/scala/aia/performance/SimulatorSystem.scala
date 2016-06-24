package aia.performance

import akka.actor.{ ActorRef, Actor }
import concurrent.duration._
import monitor.StatisticsSummary

case class SystemMessage(start: Long = 0, duration: Long = 0, id: String = "")

class ProcessRequest(serviceTime: Duration, next: ActorRef) extends Actor {
  def receive = {
    case msg: SystemMessage => {
      //simulate processing
      Thread.sleep(serviceTime.toMillis)
      next ! msg.copy(duration = System.currentTimeMillis() - msg.start, id = self.path.toString)
      //println("%s: Send message".format(self.toString()))
    }
  }
}

class ProcessCPURequest(serviceTime: Duration, next: ActorRef) extends Actor {
  def receive = {
    case msg: SystemMessage => {
      //simulate processing by doing some calculations
      var tmp = math.Pi
      var tmp2 = math.toRadians(tmp)
      val start = System.currentTimeMillis()

      do {
        for (i <- 1 until 10000) {
          tmp = tmp * 42589 / (tmp2 * 37) * 42589
          tmp2 = tmp2 * math.toDegrees(tmp)
        }
      } while (System.currentTimeMillis() - start < serviceTime.toMillis)

      next ! msg.copy(duration = System.currentTimeMillis() - msg.start, id = self.path.toString)
    }
  }
}

class PrintMsg extends Actor {
  var receivedStats: Seq[List[StatisticsSummary]] = Seq()
  def receive = {
    case "print" => {
      println("!!!!!!PRINT!!!!!!!! nr=%d".format(receivedStats.size))

      receivedStats.foreach(msg => {
        msg.filter(_.actorId.contains("Entry")).foreach(entry =>
          println("ENTRY: maxQueue=%d, utilization %02f:  %s".format(entry.maxQueueLength, entry.utilization, entry.toString)))
      })

      receivedStats.foreach(msg => {
        println("Received %s".format(msg.toString))
      })
    }
    case msg: List[StatisticsSummary] => {
      receivedStats = receivedStats :+ msg
      //simulate processing
      println("Received %s".format(msg.toString))
    }
  }
}

