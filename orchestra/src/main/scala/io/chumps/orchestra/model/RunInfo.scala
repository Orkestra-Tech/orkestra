package io.chumps.orchestra.model

import io.circe.parser._
import io.circe.generic.auto._

case class RunInfo(jobId: Symbol, runId: RunId)

object RunInfo {

  def decodeWithFallbackRunId(runInfoJson: String, fallBackRunId: => RunId) =
    decode[RunInfo](runInfoJson).fold(
      _ => RunInfo(decode[Symbol](runInfoJson).fold(throw _, identity), fallBackRunId),
      identity
    )
}
