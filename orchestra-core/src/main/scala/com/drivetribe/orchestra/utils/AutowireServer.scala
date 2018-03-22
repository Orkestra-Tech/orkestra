package com.drivetribe.orchestra.utils

import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

object AutowireServer extends autowire.Server[Json, Decoder, Encoder] {
  override def read[T: Decoder](json: Json): T = json.as[T].fold(throw _, identity)
  override def write[T: Encoder](obj: T): Json = obj.asJson
}
