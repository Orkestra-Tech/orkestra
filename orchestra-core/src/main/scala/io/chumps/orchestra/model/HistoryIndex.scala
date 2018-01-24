package io.chumps.orchestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl._

trait HistoryIndex extends Indexed {
  case class Run[ParamValues](runInfo: RunInfo,
                              paramValues: ParamValues,
                              triggeredOn: Instant,
                              parentJob: Option[RunInfo],
                              latestUpdateOn: Instant,
                              result: Option[Either[Throwable, String]],
                              tags: Seq[String])

  override def indices: Set[IndexDefinition] = super.indices + HistoryIndex

  object HistoryIndex extends IndexDefinition {
    val index = Index("history")
    val `type` = "run"

    def formatId(runInfo: RunInfo) = s"${runInfo.jobId.value}-${runInfo.runId.value}"

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          objectField("runInfo").fields(RunInfo.elasticsearchFields),
          objectField("paramValues").dynamic(false),
          dateField("triggeredOn"),
          objectField("parentJob").fields(RunInfo.elasticsearchFields),
          dateField("latestUpdateOn"),
          objectField("result").dynamic(false),
          keywordField("tags")
        )
      )
  }
}
