package com.goyeau.orchestra.model

import java.time.Instant

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.ElasticDsl._

trait StagesIndex extends Indexed {
  case class Stage(
    runInfo: RunInfo,
    parentJob: Option[RunInfo],
    name: String,
    startedOn: Instant,
    latestUpdateOn: Instant
  )

  override def indices: Set[IndexDefinition] = super.indices + StagesIndex

  object StagesIndex extends IndexDefinition {
    val index = Index("stages")
    val `type` = "stage"

    val createDefinition =
      createIndex(index.name).mappings(
        mapping(`type`).fields(
          objectField("runInfo").fields(RunInfo.elasticsearchFields),
          objectField("parentJob").fields(RunInfo.elasticsearchFields),
          textField("name"),
          dateField("startedOn"),
          dateField("latestUpdateOn")
        )
      )
  }
}
