package tech.orkestra.utils

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{HttpClient, JavaClientExceptionWrapper}
import io.circe.Encoder
import shapeless._

import tech.orkestra.OrkestraConfig
import tech.orkestra.model.Indexed._
import tech.orkestra.model.RunInfo
import tech.orkestra.utils.AkkaImplicits._

object Elasticsearch {
  def client(implicit orkestraConfig: OrkestraConfig) = HttpClient(orkestraConfig.elasticsearchUri)

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
  ) = {
    val now = Instant.now()
    indexInto(HistoryIndex.index, HistoryIndex.`type`)
      .id(HistoryIndex.formatId(runInfo))
      .source(Run[ParamValues, Unit](runInfo, paramValues, now, parent, now, None, tags))
      .createOnly(true)
  }
}
