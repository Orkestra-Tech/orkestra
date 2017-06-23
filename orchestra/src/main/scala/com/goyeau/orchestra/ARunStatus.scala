package com.goyeau.orchestra

import java.util.UUID

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

// Start with A because of a compiler bug
sealed trait ARunStatus[+Result]
object ARunStatus {
  case class Scheduled(at: Long) extends ARunStatus[Nothing]
  case class Running(at: Long) extends ARunStatus[Nothing]
  case class Success[Result](at: Long, result: Result) extends ARunStatus[Result]
  case class Failed(e: Throwable) extends ARunStatus[Nothing]
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

case class RunInfo(jobId: Symbol, runId: UUID)
