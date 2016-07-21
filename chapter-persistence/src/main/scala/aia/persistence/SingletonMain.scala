package aia.persistence

import scala.concurrent.duration._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout

import aia.persistence.rest.ShoppersServiceSupport

object SingletonMain extends App with ShoppersServiceSupport {
  implicit val system = ActorSystem("shoppers")
  val shoppers = system.actorOf(ShoppersSingleton.props,
   ShoppersSingleton.name)
  startService(shoppers)
}
