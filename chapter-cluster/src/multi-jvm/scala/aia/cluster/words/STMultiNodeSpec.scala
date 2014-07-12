package aia.cluster
package words

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest._
import org.scalatest.MustMatchers

trait STMultiNodeSpec extends MultiNodeSpecCallbacks
with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}
