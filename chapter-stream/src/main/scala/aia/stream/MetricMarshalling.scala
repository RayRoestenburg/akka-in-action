package aia.stream

import scala.util.Try
import spray.json._

trait MetricMarshalling extends EventMarshalling with DefaultJsonProtocol {
  implicit val metric = jsonFormat5(Metric)
}
