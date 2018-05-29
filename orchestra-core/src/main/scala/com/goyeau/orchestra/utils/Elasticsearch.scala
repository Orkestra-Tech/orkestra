package com.goyeau.orchestra.utils

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{HttpClient, JavaClientExceptionWrapper}
import io.circe.Encoder
import shapeless._

import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.orchestra.model.Indexed._
import com.goyeau.orchestra.model.RunInfo
import com.goyeau.orchestra.utils.AkkaImplicits._

object Elasticsearch {
  def client(implicit orchestraConfig: OrchestraConfig) = HttpClient(orchestraConfig.elasticsearchUri)

  def init()(implicit elasticsearchClient: HttpClient): Future[Unit] =
    Future
      .traverse(indices)(indexDef => elasticsearchClient.execute(indexDef.createDefinition))
      .map(_ => ())
      .recoverWith {
        case JavaClientExceptionWrapper(_) =>
          Thread.sleep(1.second.toMillis)
          init()
      }

  def indexRun[ParamValues <: HList: Encoder](
    runInfo: RunInfo,
    paramValues: ParamValues,
    tags: Seq[String],
    parent: Option[RunInfo]
  ) =
    indexInto(HistoryIndex.index, HistoryIndex.`type`)
      .id(HistoryIndex.formatId(runInfo))
      .source(Run[ParamValues, Unit](runInfo, paramValues, Instant.now(), parent, Instant.now(), None, tags))
      .createOnly(true)
}
