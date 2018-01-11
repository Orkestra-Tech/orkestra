package io.chumps.orchestra.model

import com.sksamuel.elastic4s.indexes.CreateIndexDefinition

trait IndexDefinition {
  val createDefinition: CreateIndexDefinition
}

trait Indexed {
  def indices = Set.empty[IndexDefinition]
}

object Indexed extends LogsIndex
