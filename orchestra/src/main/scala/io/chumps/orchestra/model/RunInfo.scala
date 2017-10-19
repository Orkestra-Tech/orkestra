package io.chumps.orchestra.model

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import shapeless._

import io.chumps.orchestra.Job

case class RunInfo(job: Job.Definition[_, _ <: HList, _], runId: RunId)

object RunInfo {

  implicit val encoder: Encoder[RunInfo] = runInfo =>
    Json.obj(
      "job" -> runInfo.job.asJson,
      "runId" -> runInfo.runId.asJson
  )

  implicit val decoder: Decoder[RunInfo] = c =>
    for {
      job <- c.downField("job").as[Job.Definition[_, _ <: HList, _]]
      runId <- c.downField("runId").as[RunId]
    } yield RunInfo(job, runId)

  def decodeWithFallbackRunId(runInfoJson: String, fallBackRunId: => RunId) =
    decode[RunInfo](runInfoJson).fold(
      _ => RunInfo(decode[Job.Definition[_, _ <: HList, _]](runInfoJson).fold(throw _, identity), fallBackRunId),
      identity
    )
}
