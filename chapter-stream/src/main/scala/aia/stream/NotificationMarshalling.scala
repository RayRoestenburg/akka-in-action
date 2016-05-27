package aia.stream

import scala.concurrent.duration.FiniteDuration

import scala.util.Try
import spray.json._

trait NotificationMarshalling extends EventMarshalling with DefaultJsonProtocol {
  implicit val summary = jsonFormat1(Summary)
}
