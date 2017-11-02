package io.chumps.orchestra.parameter

import io.chumps.orchestra.github.Branch

trait Encoder[T] {
  def apply(raw: String): T
}

object Encoder {
  implicit val stringEncoder: Encoder[String] = raw => raw
  implicit val intEncoder: Encoder[Int] = raw => raw.toInt
  implicit val branchEncoder: Encoder[Branch] = raw => Branch(raw)
}

trait Decoder[T] {
  def apply(o: T): String
}

object Decoder {
  implicit val stringDecoder: Decoder[String] = string => string
  implicit val intDecoder: Decoder[Int] = int => int.toString
  implicit val branchDecoder: Decoder[Branch] = branch => branch.name
}
