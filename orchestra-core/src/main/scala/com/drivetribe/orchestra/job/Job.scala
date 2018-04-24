package com.drivetribe.orchestra.job

import java.io.{IOException, PrintStream}
import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.http.scaladsl.server.Route
import autowire.Core
import com.drivetribe.orchestra.board.JobBoard
import com.goyeau.kubernetesclient.KubernetesClient
import io.circe.{Decoder, Encoder, Json}
import io.k8s.api.core.v1.PodSpec
import shapeless._
import shapeless.ops.function.FnToProduct
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.parser._
import com.drivetribe.orchestra.filesystem.Directory
import com.drivetribe.orchestra.kubernetes
import com.drivetribe.orchestra.model._
import com.drivetribe.orchestra.model.Indexed._
import com.drivetribe.orchestra.utils.{AutowireServer, Elasticsearch, ElasticsearchOutputStream}
import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.drivetribe.orchestra.{CommonApiServer, OrchestraConfig}

case class Job[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](
  board: JobBoard[ParamValues, Result, _, _],
  podSpec: ParamValues => PodSpec,
  func: ParamValues => Result
) {

  private[orchestra] def start(runInfo: RunInfo)(implicit orchestraConfig: OrchestraConfig,
                                                 kubernetesClient: KubernetesClient,
                                                 elasticsearchClient: HttpClient): Future[Result] = {
    val runningPong = system.scheduler.schedule(0.second, 1.second)(Jobs.pong(runInfo))

    (for {
      run <- elasticsearchClient
        .execute(get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(runInfo)))
        .map(
          _.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Run[ParamValues, Result]]
        )

      _ = run.parentJob.foreach { parentJob =>
        system.scheduler.schedule(1.second, 1.second) {
          CommonApiServer().runningJobs().flatMap { runningJobs =>
            if (!runningJobs.exists(_.runInfo == parentJob))
              Jobs
                .failJob(runInfo, new InterruptedException(s"Parent job ${parentJob.jobId.value} stopped"))
                .transformWith(_ => kubernetes.Jobs.delete(runInfo))
            else Future.unit
          }
        }
      }

      result = Jobs.withOutErr(
        new PrintStream(new ElasticsearchOutputStream(Elasticsearch.client, runInfo.runId), true)
      ) {
        println(s"Running job ${board.name}")
        val result =
          try func(run.paramValues)
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
      .recoverWith { case throwable => Jobs.failJob(runInfo, throwable) }
      .transformWith { triedResult =>
        for {
          _ <- Future(runningPong.cancel())
          _ <- kubernetes.Jobs.delete(runInfo)
          result <- Future.fromTry(triedResult)
        } yield result
      }
  }

  private[orchestra] case class ApiServer()(implicit orchestraConfig: OrchestraConfig,
                                            kubernetesClient: KubernetesClient,
                                            elasticsearchClient: HttpClient)
      extends board.Api {
    override def trigger(runId: RunId,
                         paramValues: ParamValues,
                         tags: Seq[String] = Seq.empty,
                         parent: Option[RunInfo] = None): Future[Unit] =
      for {
        runInfo <- Future.successful(RunInfo(board.id, runId))
        _ <- elasticsearchClient
          .execute(Elasticsearch.indexRun(runInfo, paramValues, tags, parent))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
        _ <- kubernetes.Jobs.create(runInfo, podSpec(paramValues))
      } yield ()

    override def stop(runId: RunId): Future[Unit] = kubernetes.Jobs.delete(RunInfo(board.id, runId))

    override def tags(): Future[Seq[String]] = {
      val aggregationName = "tagsForJob"
      elasticsearchClient
        .execute(
          search(HistoryIndex.index)
            .query(boolQuery.filter(termQuery("runInfo.jobId", board.id.value)))
            .aggregations(termsAgg(aggregationName, "tags"))
            .size(1000)
        )
        .map(
          _.fold(failure => throw new IOException(failure.error.reason), identity).result.aggregations
            .terms(aggregationName)
            .buckets
            .map(_.key)
        )
    }

    override def history(page: Page[Instant]): Future[History[ParamValues, Result]] =
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
          .map(
            _.fold(failure => throw new IOException(failure.error.reason), identity).result.hits.hits
              .flatMap(hit => hit.safeTo[Run[ParamValues, Result]].toOption)
          )

        stages <- if (runs.nonEmpty)
          elasticsearchClient
            .execute(
              search(StagesIndex.index)
                .query(boolQuery.filter(termsQuery("runInfo.runId", runs.map(_.runInfo.runId.value.toString).toSeq))) // TODO remove .toSeq when fixed in elastic4s
                .sortBy(fieldSort("startedOn").asc(), fieldSort("_id").desc())
                .size(1000)
            )
            .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Stage])
        else Future.successful(Seq.empty)
      } yield
        History(runs.map(run => (run, stages.filter(_.runInfo.runId == run.runInfo.runId).sortBy(_.startedOn))),
                Instant.now())
  }

  private[orchestra] def apiRoute(implicit orchestraConfig: OrchestraConfig,
                                  kubernetesClient: KubernetesClient,
                                  elasticsearchClient: HttpClient): Route = {
    import akka.http.scaladsl.server.Directives._
    path(board.id.value / Segments) { segments =>
      entity(as[String]) { entity =>
        val body = AutowireServer.read[Map[String, Json]](parse(entity).fold(throw _, identity))
        val request = board.Api.router(ApiServer()).apply(Core.Request(segments, body))
        onSuccess(request)(json => complete(json.noSpaces))
      }
    }
  }
}

object Job {

  /**
    * Create a Job.
    *
    * @param board The board that will represent this job on the UI
    */
  def apply[ParamValues <: HList, Result, Func, PodSpecFunc](
    board: JobBoard[ParamValues, Result, Func, PodSpecFunc]
  ) =
    new JobBuilder(board)

  class JobBuilder[ParamValues <: HList, Result, Func, PodSpecFunc](
    board: JobBoard[ParamValues, Result, Func, PodSpecFunc]
  ) {
    def apply(func: Directory => Func)(implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
                                       encoderP: Encoder[ParamValues],
                                       decoderP: Decoder[ParamValues],
                                       encoderR: Encoder[Result],
                                       decoderR: Decoder[Result]) =
      Job(board, (_: ParamValues) => PodSpec(Seq.empty), fnToProdFunc(func(Directory("."))))

    def apply(podSpec: PodSpec)(func: Directory => Func)(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      Job(board, (_: ParamValues) => podSpec, fnToProdFunc(func(Directory("."))))

    def apply(podSpecFunc: PodSpecFunc)(func: Directory => Func)(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      Job(board, fnToProdPodSpec(podSpecFunc), fnToProdFunc(func(Directory("."))))
  }
}
