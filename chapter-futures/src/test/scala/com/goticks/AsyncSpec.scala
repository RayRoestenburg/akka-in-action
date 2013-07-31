package com.goticks

import scala.language.postfixOps
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import scala.concurrent._
import scala.concurrent.duration._

import ExecutionContext.Implicits.global
import scala.async.Async.{async, await}

class AsyncSpec extends WordSpec with MustMatchers{


    "async" must {
      "be able to combine future results" in {

        val future = async {
          val two = async{Thread.sleep(100); 1+1}
          val three = async{Thread.sleep(400); 1+2}
          await(two) + await(three)
        }
        println("1")

        future.foreach { res =>
          res must be(5)
          println("2")
        }
        println("3")
        Await.ready(future, 600 millis)
        println("4")
      }
    }
}
