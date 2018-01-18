package io.chumps.orchestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl._

trait HistoryIndex extends Indexed {
  case class Run(runInfo: RunInfo,
                 paramValues: String,
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

    val runInfoFields = Seq(
      keywordField("jobId"),
      keywordField("runId"),
    )

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          objectField("runInfo").fields(runInfoFields),
          keywordField("paramValues"),
          dateField("triggeredOn"),
          objectField("parentJob").fields(runInfoFields),
          dateField("latestUpdateOn"),
          objectField("result").dynamic(false),
        )
      )
  }
}
