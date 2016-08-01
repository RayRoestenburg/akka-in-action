package aia.persistence

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await
import akka.NotUsed
import akka.actor._
import akka.testkit._
import org.scalatest._

import akka.stream._
import akka.stream.scaladsl._
import akka.persistence.query._
import akka.persistence.query.journal.leveldb.scaladsl._

class BasketQuerySpec extends PersistenceSpec(ActorSystem("test"))
     {

  val shopperId = 3L
  val macbookPro = Item("Apple Macbook Pro", 1, BigDecimal(2499.99))
  val macPro = Item("Apple Mac Pro", 1, BigDecimal(10499.99))
  val displays = Item("4K Display", 3, BigDecimal(2499.99))
  val appleMouse = Item("Apple Mouse", 1, BigDecimal(99.99))
  val appleKeyboard = Item("Apple Keyboard", 1, BigDecimal(79.99))
  val dWave = Item("D-Wave One", 1, BigDecimal(14999999.99))

  "Querying the journal for a basket" should {
    "return all basket events currently stored" in {
      import system.dispatcher
      val basket = system.actorOf(Basket.props, Basket.name(shopperId))
      basket ! Basket.Add(macbookPro, shopperId)
      basket ! Basket.Add(displays, shopperId)
      basket ! Basket.GetItems(shopperId)
      expectMsg(Items(macbookPro, displays))
      killActors(basket)

      implicit val mat = ActorMaterializer()(system)
      val queries = 
        PersistenceQuery(system).readJournalFor[LeveldbReadJournal](
          LeveldbReadJournal.Identifier
        )
      
      val src: Source[EventEnvelope, NotUsed] =
        queries.currentEventsByPersistenceId(
          Basket.name(shopperId), 0L, Long.MaxValue)
 
      val events: Source[Basket.Event, NotUsed] = 
        src.map(_.event.asInstanceOf[Basket.Event]) 

      val res: Future[Seq[Basket.Event]] = events.runWith(Sink.seq)
     
       Await.result(res, 10 seconds) should equal(
         Vector(
           Basket.Added(macbookPro),
           Basket.Added(displays)
         )
       )
    }
  }
}
