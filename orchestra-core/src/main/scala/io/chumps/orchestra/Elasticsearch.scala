package io.chumps.orchestra

import java.io.IOException
import java.time.Instant

import scala.concurrent.Future

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.Encoder
import io.circe.java8.time._

import io.chumps.orchestra.model.{Indexed, RunInfo}
import io.chumps.orchestra.model.Indexed._
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.utils.BaseEncoders._

object Elasticsearch {

  lazy val client = HttpClient(OrchestraConfig.elasticsearchUri)

  def init() = Future.traverse(Indexed.indices)(index => client.execute(index.createDefinition))

  def indexRun[ParamValues: Encoder](runInfo: RunInfo,
                                     paramValues: ParamValues,
                                     tags: Seq[String],
                                     parent: Option[RunInfo],
                                     refreshPolicy: RefreshPolicy) =
    for {
      run <- Future.successful(Run(runInfo, paramValues, Instant.now(), parent, Instant.now(), None, tags))
      indexResponse <- Elasticsearch.client
        .execute(
          indexInto(HistoryIndex.index, HistoryIndex.`type`)
            .id(HistoryIndex.formatId(runInfo))
            .createOnly(true)
            .source(Run(runInfo, paramValues, Instant.now(), parent, Instant.now(), None, tags))
            .refresh(refreshPolicy)
        )
      _ = indexResponse.fold(failure => throw new IOException(failure.error.reason), identity)
    } yield run
}
