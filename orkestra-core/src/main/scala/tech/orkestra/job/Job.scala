package tech.orkestra.job

import akka.http.scaladsl.server.Route
import autowire.Core
import cats.effect.{ConcurrentEffect, IO, Sync}
import cats.effect.implicits._
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.ElasticClient
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.java8.time._
import io.k8s.api.core.v1.PodSpec
import java.io.{IOException, PrintStream}
import java.time.Instant

import cats.Applicative

import scala.concurrent.Future
import scala.concurrent.duration._
import shapeless._
import tech.orkestra.board.JobBoard
import tech.orkestra.model.Indexed._
import tech.orkestra.model._
import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.utils.{AutowireServer, Elasticsearch, ElasticsearchOutputStream}
import tech.orkestra.{kubernetes, CommonApiServer, OrkestraConfig}

case class Job[F[_]: ConcurrentEffect, Parameters <: HList: Encoder: Decoder, Result: Encoder: Decoder](
  board: JobBoard[Parameters],
  podSpec: Parameters => PodSpec,
  func: Parameters => F[Result]
) {

  private[orkestra] def start(runInfo: RunInfo)(
    implicit
    orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ): F[Result] = {
    val runningPong = system.scheduler.schedule(0.second, 1.second)(Jobs.pong(runInfo))

    (for {
      run <- elasticsearchClient
        .execute(get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(runInfo)))
        .to[F]
        .map(response => response.fold(throw new IOException(response.error.reason))(_.to[Run[Parameters, Result]]))

      _ = run.parentJob.foreach { parentJob =>
        system.scheduler.schedule(1.second, 1.second) {
          IO.fromFuture(IO(CommonApiServer().runningJobs())).to[F].flatMap { runningJobs =>
            if (!runningJobs.exists(_.runInfo == parentJob))
              Jobs
                .failJob[F, Unit](runInfo, new InterruptedException(s"Parent job ${parentJob.jobId.value} stopped"))
                .guarantee(kubernetes.Jobs.delete(runInfo))
            else Applicative[F].unit
          }
        }
      }

      result = Jobs.withOutErr(
        new PrintStream(new ElasticsearchOutputStream(Elasticsearch.client, runInfo.runId), true)
      ) {
        println(s"Running job ${board.name}")
        val result =
          try func(run.parameters).toIO.unsafeRunSync()
          catch {
            case throwable: Throwable =>
              throwable.printStackTrace()
              throw throwable
          }
        println(s"Job ${board.name} completed")
        result
      }

      _ <- Jobs.succeedJob(runInfo, result)
    } yield result)
      .handleErrorWith(throwable => Jobs.failJob[F, Result](runInfo, throwable))
      .guarantee(
        Sync[F].delay(runningPong.cancel()) *>
          kubernetes.Jobs.delete(runInfo)
      )
  }

  private[orkestra] case class ApiServer()(
    implicit orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ) extends board.Api {
    override def trigger(
      runId: RunId,
      parameters: Parameters,
      tags: Seq[String] = Seq.empty,
      parent: Option[RunInfo] = None
    ): Future[Unit] =
      for {
        runInfo <- Future.successful(RunInfo(board.id, runId))
        _ <- elasticsearchClient
          .execute(Elasticsearch.indexRun(runInfo, parameters, tags, parent))
          .map(response => response.fold(throw new IOException(response.error.reason))(identity))
          .unsafeToFuture()
        _ <- ConcurrentEffect[F].toIO(kubernetes.Jobs.create[F](runInfo, podSpec(parameters))).unsafeToFuture()
      } yield ()

    override def stop(runId: RunId): Future[Unit] =
      ConcurrentEffect[F].toIO(kubernetes.Jobs.delete(RunInfo(board.id, runId))).unsafeToFuture()

    override def tags(): Future[Seq[String]] = {
      val aggregationName = "tagsForJob"
      elasticsearchClient
        .execute(
          search(HistoryIndex.index)
            .query(boolQuery.filter(termQuery("runInfo.jobId", board.id.value)))
            .aggregations(termsAgg(aggregationName, "tags"))
            .size(1000)
        )
        .map { response =>
          response.fold(throw new IOException(response.error.reason))(
            _.aggregations.terms(aggregationName).buckets.map(_.key)
          )
        }
        .unsafeToFuture()
    }

    override def history(page: Page[Instant]): Future[History[Parameters]] =
      ConcurrentEffect[F]
        .toIO(
          for {
            runs <- elasticsearchClient
              .execute(
                search(HistoryIndex.index)
                  .query(boolQuery.filter(termQuery("runInfo.jobId", board.id.value)))
                  .sortBy(fieldSort("triggeredOn").desc(), fieldSort("_id").desc())
                  .searchAfter(
                    Seq(
                      page.after
                        .getOrElse(if (page.size < 0) Instant.now() else Instant.EPOCH)
                        .toEpochMilli: java.lang.Long,
                      ""
                    )
                  )
                  .size(math.abs(page.size))
              )
              .to[F]
              .map { response =>
                response.fold(throw new IOException(response.error.reason))(
                  _.hits.hits.flatMap(hit => hit.safeTo[Run[Parameters, Result]].toOption)
                )
              }

            stages <- if (runs.nonEmpty)
              elasticsearchClient
                .execute(
                  search(StagesIndex.index)
                    .query(boolQuery.filter(termsQuery("runInfo.runId", runs.map(_.runInfo.runId.value.toString))))
                    .sortBy(fieldSort("startedOn").asc(), fieldSort("_id").desc())
                    .size(1000)
                )
                .to[F]
                .map(response => response.fold(throw new IOException(response.error.reason))(_.to[Stage]))
            else Applicative[F].pure(IndexedSeq.empty[Stage])
          } yield
            History(
              runs.map(run => (run, stages.filter(_.runInfo.runId == run.runInfo.runId).sortBy(_.startedOn))),
              Instant.now()
            )
        )
        .unsafeToFuture
  }

  private[orkestra] def apiRoute(
    implicit orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ): Route = {
    import akka.http.scaladsl.server.Directives._
    path(board.id.value / Segments) { segments =>
      entity(as[Json]) { json =>
        val body = AutowireServer.read[Map[String, Json]](json)
        val request = board.Api.router(ApiServer()).apply(Core.Request(segments, body))
        onSuccess(request)(json => complete(json))
      }
    }
  }
}

object Job {

  /**
    * Create a Job.
    *
    * @param board The board that will represent this job on the UI
    * @param func The function to execute to complete the job
    */
  def apply[F[_]: ConcurrentEffect, Parameters <: HList: Encoder: Decoder, Result: Encoder: Decoder](
    board: JobBoard[Parameters]
  )(func: => Parameters => F[Result]): Job[F, Parameters, Result] =
    Job(board, _ => PodSpec(Seq.empty), func)

  /**
    * Create a Job.
    *
    * @param board The board that will represent this job on the UI
    * @param func The function to execute to complete the job
    */
  def withPodSpec[F[_]: ConcurrentEffect, Parameters <: HList: Encoder: Decoder, Result: Encoder: Decoder](
    board: JobBoard[Parameters],
    podSpec: PodSpec
  )(func: => Parameters => F[Result]): Job[F, Parameters, Result] =
    Job(board, _ => podSpec, func)

  /**
    * Create a Job.
    *
    * @param board The board that will represent this job on the UI
    * @param func The function to execute to complete the job
    */
  def withPodSpec[F[_]: ConcurrentEffect, Parameters <: HList: Encoder: Decoder, Result: Encoder: Decoder](
    board: JobBoard[Parameters]
  )(
    podSpecFunc: => Parameters => PodSpec,
    func: => Parameters => F[Result]
  ): Job[F, Parameters, Result] =
    Job(board, podSpecFunc, func)
}
