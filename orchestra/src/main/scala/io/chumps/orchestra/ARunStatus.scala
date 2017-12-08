package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant

import scala.io.Source

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import io.circe.java8.time._

import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.model.RunInfo

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus[+Result]
object ARunStatus {
  case class Triggered(at: Instant, by: Option[RunInfo]) extends ARunStatus[Nothing]
  case class Running(at: Instant) extends ARunStatus[Nothing]
  case class Success[Result](at: Instant, result: Result) extends ARunStatus[Result]
  case class Failure(at: Instant, throwable: Throwable) extends ARunStatus[Nothing]
  case class Stopped(at: Instant) extends ARunStatus[Nothing]

  def current[Result: Decoder](runInfo: RunInfo, checkRunning: Boolean = true): Option[ARunStatus[Result]] =
    history[Result](runInfo).lastOption.map {
      case Running(_) if checkRunning && !CommonApiServer.runningJobs().contains(runInfo) => Stopped(Instant.MAX)
      case status                                                                         => status
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
