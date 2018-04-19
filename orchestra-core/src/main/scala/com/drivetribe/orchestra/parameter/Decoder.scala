package com.drivetribe.orchestra.parameter

import shapeless._

trait Decoder[T] {
  def apply(raw: String): T
}

object Decoder {
  implicit val stringDecoder: Decoder[String] = raw => raw
  implicit val intDecoder: Decoder[Int] = _.toInt
  implicit val doubleDecoder: Decoder[Double] = _.toDouble
  implicit def hlist1Decoder[T](implicit decoder: Decoder[T]): Decoder[T :: HNil] =
    raw => decoder(raw) :: HNil
  implicit def product1Decoder[Product, L <: HList](implicit generic: Generic.Aux[Product, L],
                                                    decoder: Decoder[L]): Decoder[Product] =
    raw => generic.from(decoder(raw))
}
