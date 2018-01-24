package io.chumps.orchestra

import java.time.Instant

import scala.concurrent.Future

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.Encoder
import shapeless._

import io.chumps.orchestra.model.{Indexed, RunInfo}
import io.chumps.orchestra.model.Indexed._
import io.chumps.orchestra.utils.AkkaImplicits._

object Elasticsearch {
  lazy val client = HttpClient(OrchestraConfig.elasticsearchUri)

  def init() = Future.traverse(Indexed.indices)(index => client.execute(index.createDefinition))

  def indexRun[ParamValues <: HList: Encoder](runInfo: RunInfo,
                                              paramValues: ParamValues,
                                              tags: Seq[String],
                                              parent: Option[RunInfo]) =
    indexInto(HistoryIndex.index, HistoryIndex.`type`)
      .id(HistoryIndex.formatId(runInfo))
      .source(Run[ParamValues, Nothing](runInfo, paramValues, Instant.now(), parent, Instant.now(), None, tags))
      .createOnly(true)
}
