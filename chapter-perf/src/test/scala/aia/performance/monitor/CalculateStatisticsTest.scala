package aia.performance.monitor

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import concurrent.duration._

class CalculateStatisticsTest extends WordSpecLike
  with BeforeAndAfterAll
  with MustMatchers {

  "getMailboxSummary" must {
    "calculate statistics when empty" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val stat = CalculateStatistics.processMailboxEvents(
        duration,
        Seq())
      stat.arrivalRate must be(0)
      stat.averageWaitTime must be(0)
      stat.maxQueueLength must be(0)
    }
    "calculate statistics when one event" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime = 300L
      val mailboxEvents = Seq(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime))
      val stat = CalculateStatistics.processMailboxEvents(
        duration,
        mailboxEvents)
      stat.arrivalRate must be(1)
      stat.averageWaitTime must be(waitTime.toDouble)
      stat.maxQueueLength must be(1)
    }
    "calculate statistics when multiple at same time" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime1 = 300L
      val waitTime2 = 600L
      val waitTime3 = 900L
      val mailboxEvents = Seq(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime1),
        new MailboxStatistics(
          queueSize = 2,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime2),
        new MailboxStatistics(
          queueSize = 3,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime3))
      val stat = CalculateStatistics.processMailboxEvents(
        duration,
        mailboxEvents)
      stat.arrivalRate must be(3)
      stat.averageWaitTime must be(waitTime2.toDouble)
      stat.maxQueueLength must be(3)
    }
    "calculate statistics when multiple in sequence" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime = 300L
      val mailboxEvents = Seq(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime),
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + waitTime,
          exitTime = startTime + 2 * waitTime),
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 2 * waitTime,
          exitTime = startTime + 3 * waitTime))
      val stat = CalculateStatistics.processMailboxEvents(
        duration,
        mailboxEvents)
      stat.arrivalRate must be(3)
      stat.averageWaitTime must be(waitTime.toDouble)
      stat.maxQueueLength must be(1)
    }
  }

  "getActorSummary" must {
    "calculate statistics when empty" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        Seq())
      stat.utilization must be(0)
      stat.averageServiceTime must be(0)
    }
    "calculate statistics when one event" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val serviceTime = 333L
      val actorEvents = Seq(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + serviceTime))
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        actorEvents)
      stat.utilization must be(33)
      stat.averageServiceTime must be(serviceTime)
    }
    "calculate statistics when multiple events" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val serviceTime = 333L
      val actorEvents = Seq(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + serviceTime,
          exitTime = startTime + 2 * serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 2 * serviceTime,
          exitTime = startTime + 3 * serviceTime))
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        actorEvents)
      stat.utilization must be(99)
      stat.averageServiceTime must be(serviceTime)
    }
    "calculate statistics with diferent service times" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val serviceTime1 = 100L
      val serviceTime2 = 200L
      val serviceTime3 = 300L
      val actorEvents = Seq(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + serviceTime1),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + serviceTime1,
          exitTime = startTime + serviceTime1 + serviceTime2),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + serviceTime1 + serviceTime2,
          exitTime = startTime + serviceTime1 + serviceTime2 + serviceTime3))
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        actorEvents)
      stat.utilization must be(60)
      stat.averageServiceTime must be(serviceTime2)
    }
    "calculate statistics with boundry events" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val serviceTime = 333L
      val actorEvents = Seq(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime - 100,
          exitTime = startTime + 233),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233,
          exitTime = startTime + 233 + serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + serviceTime,
          exitTime = startTime + 233 + 2 * serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + 2 * serviceTime,
          exitTime = startTime + 233 + 3 * serviceTime))
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        actorEvents)
      stat.utilization must be(100)
      stat.averageServiceTime must be(serviceTime)
    }
    "calculate statistics with one boundry event" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val actorEvents = Seq(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime - 133,
          exitTime = startTime + 200))
      val stat = CalculateStatistics.processActorEvents(
        startTime,
        duration,
        actorEvents)
      stat.utilization must be(20)
      stat.averageServiceTime must be(0)
    }
  }

  "getSummaries" must {
    "calculate one actor with only mailbox" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime1 = 300L
      val waitTime2 = 600L
      val waitTime3 = 900L
      val mailboxEvents = List(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime1),
        new MailboxStatistics(
          queueSize = 2,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime2),
        new MailboxStatistics(
          queueSize = 3,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime3))

      val mailbox = Map(
        "akka://SystemTest/system/monitoredActor" -> mailboxEvents)
      val sumList = CalculateStatistics.getSummaries(
        startTimePeriod = startTime, period = duration,
        groupedMailbox = mailbox,
        groupedActor = Map())
      sumList.size must be(1)
      val stat = sumList.head
      stat.arrivalRate must be(3)
      stat.averageWaitTime must be(waitTime2.toDouble)
      stat.maxQueueLength must be(3)
      stat.utilization must be(0)
      stat.averageServiceTime must be(0)
    }
    "calculate two actor with only mailbox" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime1 = 300L
      val waitTime2 = 600L
      val waitTime3 = 900L
      val mailboxEvents = List(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime1),
        new MailboxStatistics(
          queueSize = 2,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime2),
        new MailboxStatistics(
          queueSize = 3,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime3))

      val mailbox = Map(
        "akka://SystemTest/system/monitoredActor" -> mailboxEvents,
        "akka://SystemTest/system/monitoredActor2" -> mailboxEvents)
      val sumList = CalculateStatistics.getSummaries(
        startTimePeriod = startTime, period = duration,
        groupedMailbox = mailbox,
        groupedActor = Map())
      sumList.size must be(2)
      val stat = sumList.head
      stat.arrivalRate must be(3)
      stat.averageWaitTime must be(waitTime2.toDouble)
      stat.maxQueueLength must be(3)
      stat.utilization must be(0)
      stat.averageServiceTime must be(0)
      val stat2 = sumList.last
      stat2.arrivalRate must be(3)
      stat2.averageWaitTime must be(waitTime2.toDouble)
      stat2.maxQueueLength must be(3)
      stat2.utilization must be(0)
      stat2.averageServiceTime must be(0)
    }
    "calculate only actor statistics" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val serviceTime = 333L
      val actorEvents = List(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime - 100,
          exitTime = startTime + 233),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233,
          exitTime = startTime + 233 + serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + serviceTime,
          exitTime = startTime + 233 + 2 * serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + 2 * serviceTime,
          exitTime = startTime + 233 + 3 * serviceTime))
      val actorMap = Map(
        "akka://SystemTest/system/monitoredActor1" -> actorEvents,
        "akka://SystemTest/system/monitoredActor2" -> actorEvents)
      val sumList = CalculateStatistics.getSummaries(
        startTimePeriod = startTime, period = duration,
        groupedMailbox = Map(),
        groupedActor = actorMap)

      sumList.size must be(2)
      val stat = sumList.head
      stat.arrivalRate must be(0)
      stat.averageWaitTime must be(0)
      stat.maxQueueLength must be(0)
      stat.utilization must be(100)
      stat.averageServiceTime must be(serviceTime)
      val stat2 = sumList.last
      stat2.arrivalRate must be(0)
      stat2.averageWaitTime must be(0)
      stat2.maxQueueLength must be(0)
      stat2.utilization must be(100)
      stat2.averageServiceTime must be(serviceTime)

    }
    "calculate complete statistics" in {
      val startTime = System.currentTimeMillis()
      val duration = 1.second
      val waitTime1 = 300L
      val waitTime2 = 600L
      val waitTime3 = 900L
      val mailboxEvents = List(
        new MailboxStatistics(
          queueSize = 1,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime1),
        new MailboxStatistics(
          queueSize = 2,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime2),
        new MailboxStatistics(
          queueSize = 3,
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime,
          exitTime = startTime + waitTime3))

      val mailbox = Map(
        "akka://SystemTest/system/monitoredActor" -> mailboxEvents)
      val serviceTime = 333L
      val actorEvents = List(
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime - 100,
          exitTime = startTime + 233),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233,
          exitTime = startTime + 233 + serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + serviceTime,
          exitTime = startTime + 233 + 2 * serviceTime),
        new ActorStatistics(
          receiver = "akka://SystemTest/system/monitoredActor",
          sender = "akka://SystemTest/system/testActor1",
          entryTime = startTime + 233 + 2 * serviceTime,
          exitTime = startTime + 233 + 3 * serviceTime))
      val actorMap = Map(
        "akka://SystemTest/system/monitoredActor" -> actorEvents)

      val sumList = CalculateStatistics.getSummaries(
        startTimePeriod = startTime, period = duration,
        groupedMailbox = mailbox,
        groupedActor = actorMap)
      sumList.size must be(1)
      val stat = sumList.head
      stat.arrivalRate must be(3)
      stat.averageWaitTime must be(waitTime2.toDouble)
      stat.maxQueueLength must be(3)
      stat.utilization must be(100)
      stat.averageServiceTime must be(serviceTime)
    }

  }
}
