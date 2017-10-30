package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant

import scala.io.Source

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.model.RunInfo

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus[+Result]
object ARunStatus {
  case class Triggered[Result](at: Instant) extends ARunStatus[Result]
  case class Running[Result](at: Instant) extends ARunStatus[Result]
  case class Success[Result](at: Instant, result: Result) extends ARunStatus[Result]
  case class Failure[Result](at: Instant, throwable: Throwable) extends ARunStatus[Result]
  case object Stopped extends ARunStatus[Nothing]

  def current[Result: Decoder](runInfo: RunInfo): ARunStatus[Result] =
    history[Result](runInfo).lastOption match {
      case Some(running @ Running(_)) if CommonApiServer.runningJobs().contains(runInfo) => running
      case Some(Running(_))                                                              => ARunStatus.Stopped
      case Some(status)                                                                  => status
      case None                                                                          => throw new IllegalStateException(s"No status found for job ${runInfo.jobId} ${runInfo.runId}")
    }

  def history[Result: Decoder](runInfo: RunInfo): Seq[ARunStatus[Result]] =
    Seq(OrchestraConfig.statusFile(runInfo).toFile)
      .filter(_.exists())
      .flatMap(Source.fromFile(_).getLines())
      .map(decode[ARunStatus[Result]](_).fold(throw _, identity))

  def persist[Result: Encoder](runInfo: RunInfo, status: ARunStatus[Result]): Unit =
    Files.write(
      OrchestraConfig.statusFile(runInfo),
      s"${status.asJson.noSpaces}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
}
