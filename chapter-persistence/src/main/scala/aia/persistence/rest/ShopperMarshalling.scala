package aia.persistence.rest

import scala.util.Try

import spray.json._

import aia.persistence._

case class ItemNumber(number: Int)

trait ShopperMarshalling extends DefaultJsonProtocol {
  implicit val basketItemFormat: RootJsonFormat[Item] =
    jsonFormat3(Item)
  implicit val basketFormat: RootJsonFormat[Items] =
    jsonFormat(
      (list: List[Item]) => Items.aggregate(list), "items"
    )
  implicit val itemNumberFormat: RootJsonFormat[ItemNumber] =
    jsonFormat1(ItemNumber)
}

object ShopperMarshalling extends ShopperMarshalling
