package aia.persistence.calculator

import akka.actor._
import akka.persistence._

import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
 
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

object CalculatorHistory {
  def props = Props(new CalculatorHistory)
  def name = "calculator-history"
  case object GetHistory
  case class History(added: Int = 0, subtracted: Int = 0, divided: Int = 0, multiplied: Int = 0) {
    def incrementAdded = copy(added = added + 1)
    def incrementSubtracted= copy(subtracted = subtracted + 1)
    def incrementDivided = copy(divided = divided + 1)
    def incrementMultiplied = copy(multiplied = multiplied + 1)
  }
}

class CalculatorHistory extends Actor {
  import Calculator._
  import CalculatorHistory._

  val queries = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
    LeveldbReadJournal.Identifier)
  implicit val materializer = ActorMaterializer()
  queries.eventsByPersistenceId(Calculator.name).runWith(Sink.actorRef(self, None))

  var history = History()
  
  def receive = {
    case event: Added => history = history.incrementAdded
    case event: Subtracted => history = history.incrementSubtracted
    case event: Divided => history = history.incrementDivided
    case event: Multiplied => history = history.incrementMultiplied
    case GetHistory => sender() ! history
  }
}
