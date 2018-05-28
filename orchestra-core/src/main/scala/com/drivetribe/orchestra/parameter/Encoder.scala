package com.drivetribe.orchestra.parameter

import shapeless._

trait Encoder[T] {
  def apply(o: T): String
}

object Encoder {
  implicit val stringDecoder: Encoder[String] = string => string
  implicit val intDecoder: Encoder[Int] = _.toString
  implicit val doubleDecoder: Encoder[Double] = _.toString
  implicit def hlist1Encoder[T](implicit encoder: Encoder[T]): Encoder[T :: HNil] =
    hlist => encoder(hlist.head)
  implicit def product1Encoder[Product, L <: HList](
    implicit generic: Generic.Aux[Product, L],
    encoder: Encoder[L]
  ): Encoder[Product] =
    product => encoder(generic.to(product))
}
