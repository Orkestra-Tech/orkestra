package tech.orkestra

import java.io.IOException
import java.time.Instant

import scala.concurrent.Future
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.searches.sort.SortOrder
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import shapeless.HNil
import tech.orkestra.model.Indexed._
import tech.orkestra.model.{Page, RunId, RunInfo}
import tech.orkestra.utils.AutowireClient

trait CommonApi {
  def logs(runId: RunId, page: Page[(Instant, Int)]): Future[Seq[LogLine]]
  def runningJobs(): Future[Seq[Run[HNil, Unit]]]
}

object CommonApi {
  val client = AutowireClient(OrkestraConfig.commonSegment)[CommonApi]
}

case class CommonApiServer()(
  implicit orkestraConfig: OrkestraConfig,
  kubernetesClient: KubernetesClient,
  elasticsearchClient: ElasticClient
) extends CommonApi {
  import tech.orkestra.utils.AkkaImplicits._

  override def logs(runId: RunId, page: Page[(Instant, Int)]): Future[Seq[LogLine]] =
    elasticsearchClient
      .execute(
        search(LogsIndex.index)
          .query(boolQuery.filter(termQuery("runId", runId.value.toString)))
          .sortBy(
            fieldSort("loggedOn").order(if (page.size < 0) SortOrder.Desc else SortOrder.Asc),
            fieldSort("position").asc(),
            fieldSort("_id").desc()
          )
          .searchAfter(
            Seq(
              page.after
                .fold(if (page.size < 0) Instant.now() else Instant.EPOCH)(_._1)
                .toEpochMilli: java.lang.Long,
              page.after.fold(0)(_._2): java.lang.Integer,
              ""
            )
          )
          .size(math.abs(page.size))
      )
      .map(response => response.fold(throw new IOException(response.error.reason))(_.to[LogLine]))

  override def runningJobs(): Future[Seq[Run[HNil, Unit]]] =
    for {
      runInfos <- kubernetesClient.jobs
        .namespace(orkestraConfig.namespace)
        .list()
        .map(_.items.map(RunInfo.fromKubeJob))

      runs <- if (runInfos.nonEmpty)
        elasticsearchClient
          .execute(
            search(HistoryIndex.index)
              .query(
                boolQuery.filter(
                  termsQuery("runInfo.runId", runInfos.map(_.runId.value)),
                  termsQuery("runInfo.jobId", runInfos.map(_.jobId.value))
                )
              )
              .sortBy(fieldSort("triggeredOn").desc(), fieldSort("_id").desc())
              .size(1000)
          )
          .map(response => response.fold(throw new IOException(response.error.reason))(_.to[Run[HNil, Unit]]))
      else Future.successful(Seq.empty)
    } yield runs
}
