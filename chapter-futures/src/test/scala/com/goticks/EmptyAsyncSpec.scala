package com.goticks

import scala.language.postfixOps
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import scala.concurrent._
import scala.concurrent.duration._

import ExecutionContext.Implicits.global
import scala.async.Async.{async, await}

class AnotherAsyncSpec extends WordSpec with MustMatchers{


  "async" must {
    "be able to combine future results" in {
       val result = async {
         1 + 1
       }

    }
  }
}
