package aia.next


import akka.actor._

object Shopper {

  trait Command {
    def shopperId: Long
  }
}

