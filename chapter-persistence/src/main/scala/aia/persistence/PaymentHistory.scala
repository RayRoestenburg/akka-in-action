package aia.persistence

import akka.actor._
import akka.persistence._

import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
 
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

object PaymentHistory {
  def props(shopperId: Long) = Props(new PaymentHistory(shopperId))
  def name(shopperId: Long) = s"payment_history_${shopperId}"

  case object GetHistory

  case class History(items: List[Item] = Nil) {
    def paid(paidItems: List[Item]) = {
      History(paidItems ++ items)
    }
  }
}

class PaymentHistory(shopperId: Long) extends Actor
    with ActorLogging {
  import Basket._
  import PaymentHistory._

  val queries = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
    LeveldbReadJournal.Identifier)
  implicit val materializer = ActorMaterializer()
  queries.eventsByPersistenceId(Wallet.name(shopperId)).runWith(Sink.actorRef(self, None))

  var history = History()

  def receive = {
    case Wallet.Paid(items, _) => history = history.paid(items)
    case GetHistory => sender() ! history
  }
}
