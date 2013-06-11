package ch02
//<start id="ch02-stopsystem"/>
import org.scalatest.{ Suite, BeforeAndAfterAll }
import akka.testkit.TestKit

trait StopSystemAfterAll extends BeforeAndAfterAll { 
		//<co id="ch02-stop-system-before-and-after-all"/>
  this: TestKit with Suite => //<co id="ch02-stop-system-self-type"/>
  override protected def afterAll() {
    super.afterAll()
    system.shutdown() //<co id="ch02-stop-system-shutdown"/>
  }
}
//<end id="ch02-stopsystem"/>