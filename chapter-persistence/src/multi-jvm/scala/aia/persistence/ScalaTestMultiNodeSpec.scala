import akka.remote.testkit.MultiNodeSpecCallbacks

import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }
import org.scalatest.Matchers

trait ScalaTestMultiNodeSpec extends MultiNodeSpecCallbacks
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  override def beforeAll() = multiNodeSpecBeforeAll()
  override def afterAll() = multiNodeSpecAfterAll()
}
