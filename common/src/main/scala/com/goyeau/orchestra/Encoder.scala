package com.goyeau.orchestra

trait Encoder[T] {
  def apply(in: String): T
}

object Encoder {
  implicit val stringDecoder = new Encoder[String] {
    def apply(in: String) = in
  }
  implicit val intDecoder = new Encoder[Int] {
    def apply(in: String) = in.toInt
  }
}
