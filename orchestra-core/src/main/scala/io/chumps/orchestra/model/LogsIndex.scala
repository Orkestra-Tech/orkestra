package io.chumps.orchestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.analyzers.KeywordAnalyzer
import com.sksamuel.elastic4s.http.ElasticDsl._

trait LogsIndex extends Indexed {
  case class LogLine(runId: RunId, loggedOn: Instant, line: String, stageName: Option[String])

  override def indices: Set[IndexDefinition] = super.indices + LogsIndex

  object LogsIndex extends IndexDefinition {
    val index = Index("logs")
    val `type` = "line"

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).as(
          textField("runId").analyzer(KeywordAnalyzer),
          dateField("loggedOn"),
          textField("line"),
          textField("stageName").analyzer(KeywordAnalyzer)
        )
      )
  }
}
