package com.drivetribe.orchestra.model

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.indexes.CreateIndexDefinition

trait IndexDefinition {
  val index: Index
  val createDefinition: CreateIndexDefinition
}

trait Indexed {
  def indices = Set.empty[IndexDefinition]
}

object Indexed extends LogsIndex with HistoryIndex with StagesIndex
