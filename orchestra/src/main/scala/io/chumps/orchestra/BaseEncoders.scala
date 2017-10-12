package io.chumps.orchestra

import io.circe.{Decoder, Encoder}

object BaseEncoders {

  implicit val nothingEncoder: Encoder[Nothing] = (_: Nothing) => ???
  implicit val nothingDecoder: Decoder[Nothing] = _ => ???
}
