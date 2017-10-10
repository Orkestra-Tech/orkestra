package io.chumps.orchestra

import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant
import java.util.UUID

import io.circe.Encoder

sealed trait AStageStatus {
  def name: String
  def at: Instant
}
object AStageStatus {
  case class StageStart(name: String, at: Instant) extends AStageStatus
  case class StageEnd(name: String, at: Instant) extends AStageStatus

  def persist[Status <: AStageStatus](runId: UUID, status: Status)(implicit encoder: Encoder[AStageStatus]): Status = {
    Files.write(
      OrchestraConfig.stagesFile(runId),
      s"${AutowireServer.write[AStageStatus](status)}\n".getBytes,
      StandardOpenOption.APPEND,
      StandardOpenOption.CREATE
    )
    status
  }
}
