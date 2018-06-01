package com.goyeau.orkestra.model

import io.circe._
import io.circe.syntax._

/**
  * The id of a job.
  */
case class JobId(value: String) extends AnyVal

object JobId {
  implicit val encoder: Encoder[JobId] = _.value.asJson
  implicit val decoder: Decoder[JobId] = _.as[String].map(JobId(_))
}
