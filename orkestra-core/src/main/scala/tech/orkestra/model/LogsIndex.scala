package tech.orkestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl._

trait LogsIndex extends Indexed {
  case class LogLine(runId: RunId, loggedOn: Instant, position: Int, line: String)

  override def indices: Set[IndexDefinition] = super.indices + LogsIndex

  object LogsIndex extends IndexDefinition {
    val index = Index("logs")
    val `type` = "line"

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          keywordField("runId"),
          dateField("loggedOn"),
          intField("position"),
          textField("line")
        )
      )
  }
}
