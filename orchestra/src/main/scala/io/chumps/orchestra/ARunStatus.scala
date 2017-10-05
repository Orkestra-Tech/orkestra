package io.chumps.orchestra

import java.nio.file.attribute.FileAttribute
import java.nio.file.{FileAlreadyExistsException, Files, Paths, StandardOpenOption}
import java.time._
import java.time.temporal.ChronoUnit
import java.util.UUID

import scala.concurrent.duration._
import scala.io.Source

import akka.actor.Cancellable
import akka.stream.impl.StreamSubscriptionTimeoutSupport.NoopSubscriptionTimeout
import io.chumps.orchestra.AkkaImplicits._
import io.circe._

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus
object ARunStatus {
  case class Triggered(at: Instant) extends ARunStatus

  case class Running(at: Instant) extends ARunStatus {
    def task: Cancellable = NoopSubscriptionTimeout
    def succeed(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) = {
      task.cancel()
      RunStatusUtils.persist(runInfo, Success(Instant.now()))
    }
    def fail(runInfo: RunInfo, e: Throwable)(implicit encoder: Encoder[ARunStatus]) = {
      task.cancel()
      RunStatusUtils.persist(runInfo, Failure(Instant.now(), e))
    }
  }

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
  def current(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): ARunStatus =
    history(runInfo).lastOption match {
      case Some(s @ ARunStatus.Running(at)) if at isAfter Instant.now().minus(30, ChronoUnit.SECONDS) => s
      case Some(ARunStatus.Running(_))                                                                => ARunStatus.Stopped
      case Some(s)                                                                                    => s
      case None                                                                                       => throw new IllegalStateException(s"No status found for job ${runInfo.jobId} ${runInfo.runId}")
    }

  def history(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): Seq[ARunStatus] =
    Source
      .fromFile(OrchestraConfig.statusFile(runInfo).toFile)
      .getLines()
      .map(AutowireServer.read[ARunStatus])
      .toSeq

  def runInit(runInfo: RunInfo, tags: Seq[String]) = {
    val runDir = OrchestraConfig.runDir(runInfo)
    val firstTimeInit = runDir.toFile.mkdirs()

    if (firstTimeInit) {
      OrchestraConfig.logsDir(runInfo.runId).toFile.mkdirs()

      tags.foreach { tag =>
        val tagDir = OrchestraConfig.tagDir(runInfo.jobId, tag)
        tagDir.toFile.mkdirs()
        Files.createSymbolicLink(Paths.get(tagDir.toString, runInfo.runId.toString), runDir)
      }

      val now = LocalDateTime.now()
      val dateDir = Paths
        .get(OrchestraConfig.runsDirByDate(runInfo.jobId).toString,
             now.getYear.toString,
             now.getDayOfYear.toString,
             now.toEpochSecond(ZoneOffset.UTC).toString)
      dateDir.toFile.mkdirs()
      Files.createSymbolicLink(Paths.get(dateDir.toString, runInfo.runId.toString), runDir)
    }
  }

  def notifyTriggered(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) =
    RunStatusUtils.persist(runInfo, ARunStatus.Triggered(Instant.now()))

  def notifyRunning(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) = new ARunStatus.Running(Instant.now()) {
    override val task =
      system.scheduler.schedule(0.second, 15.seconds)(
        RunStatusUtils.persist(runInfo, ARunStatus.Running(Instant.now()))
      )
  }

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

case class RunInfo(jobId: Symbol, runIdMaybe: Option[UUID]) {
  val runId = runIdMaybe.getOrElse(UUID.randomUUID())
}
