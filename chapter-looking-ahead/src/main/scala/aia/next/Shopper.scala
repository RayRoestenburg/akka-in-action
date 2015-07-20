package aia.next
//<start id="persistence-shopper"/>

import akka.actor._

object Shopper {

  trait Command {
    def shopperId: Long //<co id="shopper_command"/>
  }
}

