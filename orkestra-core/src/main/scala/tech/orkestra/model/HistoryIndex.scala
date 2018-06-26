package tech.orkestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.parser._
import cats.implicits._
import io.circe.CursorOp.DownField
import shapeless.HList

import tech.orkestra.utils.BaseEncoders._

trait HistoryIndex extends Indexed {
  case class Run[ParamValues <: HList, Result](
    runInfo: RunInfo,
    paramValues: ParamValues,
    triggeredOn: Instant,
    parentJob: Option[RunInfo],
    latestUpdateOn: Instant,
    result: Option[Either[Throwable, Result]],
    tags: Seq[String]
  )

  object Run {
    implicit def decoder[ParamValues <: HList: Decoder, Result: Decoder]: Decoder[Run[ParamValues, Result]] =
      cursor =>
        for {
          runInfo <- cursor.downField("runInfo").as[RunInfo]
          paramValuesString <- cursor.downField("paramValues").as[String]
          paramValues <- decode[ParamValues](paramValuesString)
            .leftMap(failure => DecodingFailure(failure.getMessage, List(DownField("paramValues"))))
          triggeredOn <- cursor.downField("triggeredOn").as[Instant]
          parentJob <- cursor.downField("parentJob").as[Option[RunInfo]]
          latestUpdateOn <- cursor.downField("latestUpdateOn").as[Instant]
          result <- cursor.downField("result").as[Option[Either[Throwable, Result]]]
          tags <- cursor.downField("tags").as[Seq[String]]
        } yield Run(runInfo, paramValues, triggeredOn, parentJob, latestUpdateOn, result, tags)

    implicit def encoder[ParamValues <: HList: Encoder, Result: Encoder]: Encoder[Run[ParamValues, Result]] =
      run =>
        Json.obj(
          "runInfo" -> run.runInfo.asJson,
          "paramValues" -> Json.fromString(run.paramValues.asJson.noSpaces),
          "triggeredOn" -> run.triggeredOn.asJson,
          "parentJob" -> run.parentJob.asJson,
          "latestUpdateOn" -> run.latestUpdateOn.asJson,
          "result" -> run.result.asJson,
          "tags" -> run.tags.asJson
        )
  }

  override def indices: Set[IndexDefinition] = super.indices + HistoryIndex

  object HistoryIndex extends IndexDefinition {
    val index = Index("history")
    val `type` = "run"

    def formatId(runInfo: RunInfo) = s"${runInfo.jobId.value}-${runInfo.runId.value}"

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          objectField("runInfo").fields(RunInfo.elasticsearchFields),
          textField("paramValues"),
          dateField("triggeredOn"),
          objectField("parentJob").fields(RunInfo.elasticsearchFields),
          dateField("latestUpdateOn"),
          objectField("result").dynamic(false),
          keywordField("tags")
        )
      )
  }
}
