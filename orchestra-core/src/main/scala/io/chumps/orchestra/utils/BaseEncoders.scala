package io.chumps.orchestra.utils

import io.circe.{Decoder, Encoder, Json}

object BaseEncoders {
  implicit val encodeNothing: Encoder[Nothing] = _ => ???
  implicit val decodeNothing: Decoder[Nothing] = _ => ???

  implicit val encodeThrowable: Encoder[Throwable] = throwable =>
    Json.obj("message" -> Json.fromString(throwable.getMessage))
  implicit val decodeThrowable: Decoder[Throwable] = c =>
    for {
      message <- c.downField("message").as[String]
    } yield new Throwable(message)
}
