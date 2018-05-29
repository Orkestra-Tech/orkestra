package com.goyeau.orchestra.utils

import io.circe.{Decoder, Encoder, Json}

object BaseEncoders {
  implicit val encodeThrowable: Encoder[Throwable] = throwable =>
    Json.obj("message" -> Json.fromString(Option(throwable.getMessage).getOrElse("")))
  implicit val decodeThrowable: Decoder[Throwable] = cursor =>
    cursor.downField("message").as[String].map(new Throwable(_))
}
