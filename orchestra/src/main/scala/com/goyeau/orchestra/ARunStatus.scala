package com.goyeau.orchestra

import java.nio.file.attribute.FileAttribute
import java.nio.file.{FileAlreadyExistsException, Files, Paths, StandardOpenOption}
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import scala.concurrent.duration._
import scala.io.Source

import akka.actor.Cancellable
import akka.stream.impl.StreamSubscriptionTimeoutSupport.NoopSubscriptionTimeout
import com.goyeau.orchestra.AkkaImplicits._
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
      case Some(s @ ARunStatus.Running(at)) if at isAfter Instant.now().minus(2, ChronoUnit.MINUTES) => s
      case Some(ARunStatus.Running(_))                                                               => ARunStatus.Stopped
      case Some(s)                                                                                   => s
      case None                                                                                      => throw new IllegalStateException(s"No status found for job ${runInfo.jobId} ${runInfo.runId}")
    }

  def history(runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus]): Seq[ARunStatus] =
    Source
      .fromFile(OrchestraConfig.statusFilePath(runInfo).toFile)
      .getLines()
      .map(AutowireServer.read[ARunStatus])
      .toSeq

  def runPrerequisites(runInfo: RunInfo, tags: Seq[String]) = {
    OrchestraConfig.runDirPath(runInfo).toFile.mkdirs()
    OrchestraConfig.logsDirPath(runInfo.runId).toFile.mkdirs()

    tags.foreach { tag =>
      val tagDir = OrchestraConfig.tagDirPath(runInfo.jobId, tag)
      tagDir.toFile.mkdirs()
      try Files.createSymbolicLink(Paths.get(tagDir.toString, runInfo.runId.toString),
                                   OrchestraConfig.runDirPath(runInfo))
      catch { case _: FileAlreadyExistsException => }
    }
  }

  def notifyTriggered(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) =
    RunStatusUtils.persist(runInfo, ARunStatus.Triggered(Instant.now()))

  def notifyRunning(runInfo: RunInfo)(implicit encoder: Encoder[ARunStatus]) = new ARunStatus.Running(Instant.now()) {
    override val task =
      system.scheduler.schedule(0.second, 1.minute)(RunStatusUtils.persist(runInfo, ARunStatus.Running(Instant.now())))
  }

  def persist[Status <: ARunStatus](runInfo: RunInfo,
                                    status: Status)(implicit encoder: Encoder[ARunStatus]): Status = {
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
