package com.goyeau.orchestra

import _root_.io.circe.{Decoder, Encoder}
import _root_.io.circe.syntax._
import _root_.io.circe.parser._

object AutowireServer extends autowire.Server[String, Decoder, Encoder] {
  override def read[T: Decoder](json: String): T = decode[T](json).fold(throw _, identity)
  override def write[T: Encoder](obj: T): String = obj.asJson.noSpaces
}
