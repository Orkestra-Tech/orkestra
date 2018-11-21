package tech.orkestra.model

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.indexes.CreateIndexRequest

trait IndexDefinition {
  val index: Index
  val createIndexRequest: CreateIndexRequest
}

trait Indexed {
  def indices = Set.empty[IndexDefinition]
}

object Indexed extends LogsIndex with HistoryIndex with StagesIndex
