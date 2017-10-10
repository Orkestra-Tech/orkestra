package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

import io.chumps.orchestra.AkkaImplicits._
import io.circe._
import io.circe.parser._
import io.circe.shapes._
import io.circe.syntax._
import shapeless._

import io.chumps.orchestra.kubernetes.{JobUtils, Kubernetes}

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus
object ARunStatus {
  case class Triggered(at: Instant) extends ARunStatus

  case class Running(at: Instant) extends ARunStatus {
    def succeed(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) =
      persist(runInfo, Success(Instant.now()))

    def fail(runInfo: RunInfo, e: Throwable)(implicit encoder: Encoder[ARunStatus]) =
      persist(runInfo, Failure(Instant.now(), e))
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

  def current(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): ARunStatus =
    history(runInfo).lastOption match {
      case Some(running @ ARunStatus.Running(_))
          if Await.result(
            Kubernetes.client.jobs
              .namespace(OrchestraConfig.namespace)
              .list()
              .map(_.items.exists(_.metadata.get.name.get == JobUtils.jobName(runInfo))),
            Duration.Inf
          ) =>
        running
      case Some(ARunStatus.Running(_)) => ARunStatus.Stopped
      case Some(status)                => status
      case None                        => throw new IllegalStateException(s"No status found for job ${runInfo.job.id} ${runInfo.runId}")
    }

  def history(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): Seq[ARunStatus] =
    Source
      .fromFile(OrchestraConfig.statusFile(runInfo).toFile)
      .getLines()
      .map(AutowireServer.read[ARunStatus])
      .toSeq

  def persist[Status <: ARunStatus](runInfo: RunInfo,
                                    status: Status)(implicit encoder: Encoder[ARunStatus]): Status = {
    Files.write(
      OrchestraConfig.statusFile(runInfo),
      s"${AutowireServer.write[ARunStatus](status)}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    status
  }
}
