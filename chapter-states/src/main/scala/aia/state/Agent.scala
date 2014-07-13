package aia.state

import akka.agent.Agent
import akka.actor.ActorSystem
import concurrent.Await
import concurrent.duration._
import akka.util.Timeout
//import concurrent.ExecutionContext.Implicits.global

//<start id="ch10-agent-state"/>
case class BookStatics(val nameBook: String, nrSold: Int)
case class StateBookStatics(val sequence: Long,
                            books: Map[String, BookStatics])
//<end id="ch10-agent-state"/>

class BookStaticsMgr(system: ActorSystem) {
  implicit val ex = system.dispatcher //todo: change chapter 2.2 =>2.3
  val stateAgent = Agent(new StateBookStatics(0, Map())) //todo: change chapter 2.2 =>2.3

  def addBooksSold(book: String, nrSold: Int) {
    stateAgent send (oldState => {
      val bookStat = oldState.books.get(book) match {
        case Some(bookState) =>
          bookState.copy(nrSold = bookState.nrSold + nrSold)
        case None => new BookStatics(book, nrSold)
      }
      oldState.copy(oldState.sequence + 1,
        oldState.books + (book -> bookStat))
    })
  }

  def addBooksSoldAndReturnNewState(book: String,
                                    nrSold: Int): StateBookStatics = {
    val future = stateAgent alter (oldState => {
      val bookStat = oldState.books.get(book) match {
        case Some(bookState) =>
          bookState.copy(nrSold = bookState.nrSold + nrSold)
        case None => new BookStatics(book, nrSold)
      }
      oldState.copy(oldState.sequence + 1,
        oldState.books + (book -> bookStat))
    })
    Await.result(future, 1 second)
  }

  def getStateBookStatics(): StateBookStatics = {
    stateAgent.get()
  }
}
