package com.goyeau.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant
import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.kubernetes.{JobUtils, Kubernetes}
import io.circe._

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus
object ARunStatus {
  case class Triggered(at: Instant) extends ARunStatus
  case class Running(at: Instant) extends ARunStatus
  case class Success(at: Instant) extends ARunStatus
  case class Failure(at: Instant, e: Throwable) extends ARunStatus
  case object Stopped extends ARunStatus

  // Circe encoders/decoders
  implicit val encodeThrowable = new Encoder[Throwable] {
    final def apply(o: Throwable): Json = Json.obj(
      ("message", Json.fromString(o.getMessage))
    )
  }

  implicit val decodeThrowable = new Decoder[Throwable] {
    final def apply(c: HCursor): Decoder.Result[Throwable] =
      for {
        message <- c.downField("message").as[String]
      } yield new Throwable(message)
  }
}

object RunStatusUtils {
  def current(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus], encoder: Encoder[ARunStatus]): ARunStatus =
    history(runInfo).lastOption match {
      case Some(s: ARunStatus.Running) =>
        // Check if this job is still running or a ghost
        val kubeJob = Kubernetes.client
          .namespaces(OrchestraConfig.namespace)
          .jobs(JobUtils.jobName(runInfo))
          .get()

        val jobRunning = kubeJob.map(_ => s)
        val jobStopped = kubeJob.failed.map(_ => ARunStatus.Stopped)
        Await.result(jobRunning.fallbackTo(jobStopped), Duration.Inf)
      case Some(s) => s
      case None    => throw new IllegalStateException(s"No status found for job ${runInfo.jobId} ${runInfo.runId}")
    }

  def history(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): Seq[ARunStatus] =
    Source
      .fromFile(OrchestraConfig.statusFilePath(runInfo).toFile)
      .getLines()
      .map(AutowireServer.read[ARunStatus])
      .toSeq

  def save(runInfo: RunInfo, status: ARunStatus)(implicit encoder: Encoder[ARunStatus]): ARunStatus = {
    Files.write(
      OrchestraConfig.statusFilePath(runInfo),
      s"${AutowireServer.write[ARunStatus](status)}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    status
  }
}

case class RunInfo(jobId: Symbol, runIdMaybe: Option[UUID]) {
  val runId = runIdMaybe.getOrElse(UUID.randomUUID())
}
