package aia.persistence.rest

import scala.util.Try

import spray.json._
import spray.httpx.unmarshalling._

import aia.persistence._

case class ItemNumber(number: Int)

trait ShopperMarshalling extends DefaultJsonProtocol {
  implicit val basketItemFormat: RootJsonFormat[Basket.Item] =
    jsonFormat3(Basket.Item)
  implicit val basketFormat: RootJsonFormat[Basket.Items] =
    jsonFormat(
      (list:List[Basket.Item]) => Basket.Items.aggregate(list), "items"
    )
  implicit val itemNumberFormat: RootJsonFormat[ItemNumber] =
    jsonFormat1(ItemNumber)
}

object ShopperMarshalling extends ShopperMarshalling
