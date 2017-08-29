package com.goyeau.orchestra

import java.time.Instant
import java.util.UUID

import scala.io.Source

import io.circe._

// Start with A because of a compiler bug
// Should be in the model package
sealed trait ARunStatus[+Result]
object ARunStatus {
  case class Scheduled(at: Instant) extends ARunStatus[Nothing]
  case class Running(at: Instant) extends ARunStatus[Nothing]
  case class Success[Result](at: Instant, result: Result) extends ARunStatus[Result]
  case class Failure(e: Throwable) extends ARunStatus[Nothing]
  case object Unknown extends ARunStatus[Nothing]

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
  def load[Result](runInfo: RunInfo)(implicit decoder: Decoder[ARunStatus[Result]]): Seq[ARunStatus[Result]] =
    Source
      .fromFile(OrchestraConfig.statusFilePath(runInfo))
      .getLines()
      .map(AutowireServer.read[ARunStatus[Result]])
      .toSeq
}

case class RunInfo(jobId: Symbol, runIdMaybe: Option[UUID]) {
  val runId = runIdMaybe.getOrElse(UUID.randomUUID())
}
