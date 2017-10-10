package io.chumps.orchestra

import java.util.UUID

import io.circe._
import io.circe.syntax._
import shapeless._

case class RunInfo(job: Job.Definition[_, _ <: HList, _], runIdMaybe: Option[UUID]) {
  lazy val runId = runIdMaybe.getOrElse(UUID.fromString(OrchestraConfig.jobUid))
}

object RunInfo {

  // Circe encoders/decoders
  implicit val encodeNothing = new Encoder[Nothing] {
    final def apply(o: Nothing): Json = ???
  }

  implicit val decodeNothing = new Decoder[Nothing] {
    final def apply(c: HCursor): Decoder.Result[Nothing] = ???
  }

  implicit val encodeRunInfo = new Encoder[RunInfo] {
    final def apply(o: RunInfo): Json = Json.obj(
      "job" -> Json.obj(
        "id" -> o.job.id.asJson,
        "name" -> Json.fromString(o.job.name)
      ),
      "runIdMaybe" -> o.runIdMaybe.asJson
    )
  }

  implicit val decodeRunInfo = new Decoder[RunInfo] {
    final def apply(c: HCursor): Decoder.Result[RunInfo] =
      for {
        id <- c.downField("job").downField("id").as[Symbol]
        name <- c.downField("job").downField("name").as[String]
        job = Job.Definition[Nothing, HNil, Nothing](id, name)
        runIdMaybe <- c.downField("runIdMaybe").as[Option[UUID]]
      } yield RunInfo(job, runIdMaybe)
  }
}
