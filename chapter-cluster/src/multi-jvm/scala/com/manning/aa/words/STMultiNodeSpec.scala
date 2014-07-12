package com.manning.aa
package words

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest._
import org.scalatest.matchers.MustMatchers

trait STMultiNodeSpec extends MultiNodeSpecCallbacks
with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}
