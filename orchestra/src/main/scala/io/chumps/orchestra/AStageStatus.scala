package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant
import java.util.UUID

import scala.io.Source

import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._

// Start with A because of a compiler bug
// Should be in the model package
sealed trait AStageStatus {
  def name: String
  def at: Instant
}
object AStageStatus {
  case class StageStart(name: String, at: Instant) extends AStageStatus
  case class StageEnd(name: String, at: Instant) extends AStageStatus

  def history(runId: UUID): Seq[AStageStatus] =
    Seq(OrchestraConfig.stagesFile(runId).toFile)
      .filter(_.exists())
      .flatMap(Source.fromFile(_).getLines())
      .map(AutowireServer.read[AStageStatus])

  def persist[Status <: AStageStatus](runId: UUID, status: Status): Status = {
    Files.write(
      OrchestraConfig.stagesFile(runId),
      s"${AutowireServer.write[AStageStatus](status)}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    status
  }
}
