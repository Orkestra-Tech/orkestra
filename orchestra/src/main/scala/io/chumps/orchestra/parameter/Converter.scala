package io.chumps.orchestra.parameter

trait Converter[T] {
  def apply(raw: String): T
}

object Converter {
  implicit val stringConverter: Converter[String] = raw => raw
  implicit val intConverter: Converter[Int] = raw => raw.toInt
}
