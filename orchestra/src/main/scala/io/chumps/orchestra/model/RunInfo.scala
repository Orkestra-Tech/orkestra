package io.chumps.orchestra.model

import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class RunInfo(jobId: Symbol, runId: RunId)

object RunInfo {

  def decodeWithFallbackRunId(runInfoJson: String, fallBackRunId: => RunId) =
    decode[RunInfo](runInfoJson).fold(
      _ => RunInfo(decode[Symbol](runInfoJson).fold(throw _, identity), fallBackRunId),
      identity
    )

  implicit val encoder: Encoder[RunInfo] = deriveEncoder
  implicit val decoder: Decoder[RunInfo] = deriveDecoder
}
