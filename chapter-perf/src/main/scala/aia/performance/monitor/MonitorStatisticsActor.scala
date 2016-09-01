package aia.performance.monitor

import akka.actor.{ ActorRef, Actor }
import concurrent.duration.Duration

class MonitorStatisticsActor(period: Duration, processMargin: Long,
                             storeSummaries: ActorRef) extends Actor {
  private var periodNr = System.currentTimeMillis() / period.toMillis
  private var listMailbox = List[MailboxStatistics]()
  private var listActor = List[ActorStatistics]()

  def receive = {
    case mailbox: MailboxStatistics => {
      listMailbox = listMailbox :+ mailbox
      process(mailbox.exitTime)
    }
    case actor: ActorStatistics => {
      listActor = listActor :+ actor
      process(actor.exitTime)
    }
  }

  private def process(processTime: Long): Unit = {
    val exceededPeriod = processTime % period.toMillis
    //make sure we have received all events for each period
    val currentPeriod = if (exceededPeriod > processMargin)
      processTime / period.toMillis
    else
      processTime / period.toMillis - 1

    if (currentPeriod > periodNr) {
      //previous period ended => do processing
      for (nr <- periodNr until currentPeriod) {
        processPeriod(nr)
      }

      //update lists
      val endTimePeriod = currentPeriod * period.toMillis
      listMailbox = listMailbox.filter(_.exitTime >= endTimePeriod)
      listActor = listActor.filter(_.exitTime >= endTimePeriod)
      periodNr = currentPeriod
    }
  }

  private def processPeriod(periodNr: Long): Unit = {
    val startTimePeriod = periodNr * period.toMillis
    val endTimePeriod = startTimePeriod + period.toMillis
    //filter events for period
    //use only events that are started in period
    val mailbox = listMailbox.filter(event =>
      event.entryTime >= startTimePeriod &&
        event.entryTime < endTimePeriod)
    //group events by actor
    val groupedMailbox = mailbox.groupBy(_.receiver)

    val actor = listActor.filterNot(event =>
      event.exitTime <= startTimePeriod ||
        event.entryTime >= endTimePeriod)
    //group events by actor
    val groupedActor = actor.groupBy(_.receiver)

    val summaries = CalculateStatistics.getSummaries(
      startTimePeriod, period,
      groupedMailbox, groupedActor)
    storeSummaries ! summaries
  }

}
