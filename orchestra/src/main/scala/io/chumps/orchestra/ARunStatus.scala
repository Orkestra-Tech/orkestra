package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

import io.circe._
import io.circe.parser._
import io.circe.shapes._
import io.circe.generic.auto._
import io.circe.java8.time._

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus
object ARunStatus {
  case class Triggered(at: Instant) extends ARunStatus

  case class Running(at: Instant) extends ARunStatus {
    def succeed(runInfo: RunInfo) = persist(runInfo, Success(Instant.now()))

    def fail(runInfo: RunInfo, e: Throwable) = persist(runInfo, Failure(Instant.now(), e))
  }

  case class Success(at: Instant) extends ARunStatus

  case class Failure(at: Instant, e: Throwable) extends ARunStatus

  case object Stopped extends ARunStatus

  // Circe encoders/decoders
  implicit val encodeThrowable = new Encoder[Throwable] {
    final def apply(o: Throwable): Json = Json.obj(
      "message" -> Json.fromString(o.getMessage)
    )
  }

  implicit val decodeThrowable = new Decoder[Throwable] {
    final def apply(c: HCursor): Decoder.Result[Throwable] =
      for {
        message <- c.downField("message").as[String]
      } yield new Throwable(message)
  }

  def current(runInfo: RunInfo): ARunStatus =
    history(runInfo).lastOption match {
      case Some(running @ ARunStatus.Running(_)) if CommonApiServer.runningJobs().contains(runInfo) => running
      case Some(ARunStatus.Running(_)) =>
        println("CommonApiServer.runningJobs(): " + CommonApiServer.runningJobs())
        println("runInfo: " + runInfo)
        ARunStatus.Stopped
      case Some(status) => status
      case None         => throw new IllegalStateException(s"No status found for job ${runInfo.job.id} ${runInfo.runId}")
    }

  def history(runInfo: RunInfo): Seq[ARunStatus] =
    Seq(OrchestraConfig.statusFile(runInfo).toFile)
      .filter(_.exists())
      .flatMap(Source.fromFile(_).getLines())
      .map(AutowireServer.read[ARunStatus])

  def persist[Status <: ARunStatus](runInfo: RunInfo, status: Status): Status = {
    Files.write(
      OrchestraConfig.statusFile(runInfo),
      s"${AutowireServer.write[ARunStatus](status)}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    status
  }
}
