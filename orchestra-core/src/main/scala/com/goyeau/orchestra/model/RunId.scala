package com.goyeau.orchestra.model

import java.util.UUID

import io.circe._
import io.circe.syntax._

/**
  * The id of a run.
  */
case class RunId(value: UUID) extends AnyVal

object RunId {
  def apply(raw: String): RunId = RunId(UUID.fromString(raw))
  def random() = RunId(UUID.randomUUID())

  implicit val encoder: Encoder[RunId] = _.value.asJson
  implicit val decoder: Decoder[RunId] = _.as[UUID].map(RunId(_))
}
