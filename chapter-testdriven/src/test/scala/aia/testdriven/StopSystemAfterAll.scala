package aia.testdriven
//<start id="ch02-stopsystem"/>
import org.scalatest.{ Suite, BeforeAndAfterAll }
import akka.testkit.TestKit

trait StopSystemAfterAll extends BeforeAndAfterAll { 
		//<co id="ch02-stop-system-before-and-after-all"/>
  this: TestKit with Suite => //<co id="ch02-stop-system-self-type"/>
  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate() //<co id="ch02-stop-system-terminate"/>
  }
}
//<end id="ch02-stopsystem"/>
