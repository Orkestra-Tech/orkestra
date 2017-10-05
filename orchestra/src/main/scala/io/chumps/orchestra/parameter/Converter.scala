package io.chumps.orchestra.parameter

trait Converter[T] {
  def apply(raw: String): T
}

object Converter {
  implicit val stringConverter = new Converter[String] {
    def apply(raw: String) = raw
  }

  implicit val intConverter = new Converter[Int] {
    def apply(raw: String) = raw.toInt
  }
}
