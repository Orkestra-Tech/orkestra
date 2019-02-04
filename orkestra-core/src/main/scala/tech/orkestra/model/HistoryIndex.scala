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
  case class Run[Parameters <: HList, Result](
    runInfo: RunInfo,
    parameters: Parameters,
    triggeredOn: Instant,
    parentJob: Option[RunInfo],
    latestUpdateOn: Instant,
    result: Option[Either[Throwable, Result]],
    tags: Seq[String]
  )

  object Run {
    implicit def decoder[Parameters <: HList: Decoder, Result: Decoder]: Decoder[Run[Parameters, Result]] =
      cursor =>
        for {
          runInfo <- cursor.downField("runInfo").as[RunInfo]
          parametersString <- cursor.downField("parameters").as[String]
          parameters <- decode[Parameters](parametersString)
            .leftMap(failure => DecodingFailure(failure.getMessage, List(DownField("parameters"))))
          triggeredOn <- cursor.downField("triggeredOn").as[Instant]
          parentJob <- cursor.downField("parentJob").as[Option[RunInfo]]
          latestUpdateOn <- cursor.downField("latestUpdateOn").as[Instant]
          result <- cursor.downField("result").as[Option[Either[Throwable, Result]]]
          tags <- cursor.downField("tags").as[Seq[String]]
        } yield Run(runInfo, parameters, triggeredOn, parentJob, latestUpdateOn, result, tags)

    implicit def encoder[Parameters <: HList: Encoder, Result: Encoder]: Encoder[Run[Parameters, Result]] =
      run =>
        Json.obj(
          "runInfo" -> run.runInfo.asJson,
          "parameters" -> Json.fromString(run.parameters.asJson.noSpaces),
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

    val createIndexRequest =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          objectField("runInfo").fields(RunInfo.elasticsearchFields),
          textField("parameters"),
          dateField("triggeredOn"),
          objectField("parentJob").fields(RunInfo.elasticsearchFields),
          dateField("latestUpdateOn"),
          objectField("result").dynamic(false),
          keywordField("tags")
        )
      )
  }
}
