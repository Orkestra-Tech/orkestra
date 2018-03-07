package io.chumps.orchestra.job

import java.io.{IOException, PrintStream}
import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.http.scaladsl.server.Route
import autowire.Core
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

import io.chumps.orchestra.board.Job
import io.chumps.orchestra.filesystem.Directory
import io.chumps.orchestra.kubernetes.Jobs
import io.chumps.orchestra.model._
import io.chumps.orchestra.model.Indexed._
import io.chumps.orchestra.utils.{AutowireServer, Elasticsearch, ElasticsearchOutputStream}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.{CommonApiServer, OrchestraConfig}

case class JobRunner[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](
  job: Job[ParamValues, Result, _, _],
  podSpec: ParamValues => PodSpec,
  func: ParamValues => Result
) {

  private[orchestra] def start(runInfo: RunInfo)(implicit orchestraConfig: OrchestraConfig,
                                                 kubernetesClient: KubernetesClient,
                                                 elasticsearchClient: HttpClient): Future[Result] = {
    val runningPong = system.scheduler.schedule(0.second, 1.second)(JobRunners.pong(runInfo))

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
              JobRunners
                .failJob(runInfo, new InterruptedException(s"Parent job ${parentJob.jobId.value} stopped"))
                .transformWith(_ => Jobs.delete(runInfo))
            else Future.unit
          }
        }
      }

      result = JobRunners.withOutErr(
        new PrintStream(new ElasticsearchOutputStream(Elasticsearch.client, runInfo.runId), true)
      ) {
        println(s"Running job ${job.name}")
        val result =
          try func(run.paramValues)
          catch {
            case throwable: Throwable =>
              throwable.printStackTrace()
              throw throwable
          }
        println(s"Job ${job.name} completed")
        result
      }

      _ <- JobRunners.succeedJob(runInfo, result)
    } yield result)
      .recoverWith { case throwable => JobRunners.failJob(runInfo, throwable) }
      .transformWith { triedResult =>
        for {
          _ <- Future(runningPong.cancel())
          _ <- Jobs.delete(runInfo)
          result <- Future.fromTry(triedResult)
        } yield result
      }
  }

  private[orchestra] case class ApiServer()(implicit orchestraConfig: OrchestraConfig,
                                            kubernetesClient: KubernetesClient,
                                            elasticsearchClient: HttpClient)
      extends job.Api {
    override def trigger(runId: RunId,
                         paramValues: ParamValues,
                         tags: Seq[String] = Seq.empty,
                         parent: Option[RunInfo] = None): Future[Unit] =
      for {
        runInfo <- Future.successful(RunInfo(job.id, runId))
        _ <- elasticsearchClient
          .execute(Elasticsearch.indexRun(runInfo, paramValues, tags, parent))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
        _ <- Jobs.create(runInfo, podSpec(paramValues))
      } yield ()

    override def stop(runId: RunId): Future[Unit] = Jobs.delete(RunInfo(job.id, runId))

    override def tags(): Future[Seq[String]] = {
      val aggregationName = "tagsForJob"
      elasticsearchClient
        .execute(
          search(HistoryIndex.index)
            .query(boolQuery.filter(termQuery("runInfo.jobId", job.id.value)))
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
              .query(boolQuery.filter(termQuery("runInfo.jobId", job.id.value)))
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
                .query(boolQuery.filter(termsQuery("runInfo.runId", runs.toSeq.map(_.runInfo.runId.value)))) // TODO remove .toSeq when fixed in elastic4s
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
    path(job.id.value / Segments) { segments =>
      entity(as[String]) { entity =>
        val body = AutowireServer.read[Map[String, Json]](parse(entity).fold(throw _, identity))
        val request = job.Api.router(ApiServer()).apply(Core.Request(segments, body))
        onSuccess(request)(json => complete(json.noSpaces))
      }
    }
  }
}

object JobRunner {

  def apply[ParamValues <: HList, Result, Func, PodSpecFunc](job: Job[ParamValues, Result, Func, PodSpecFunc]) =
    new JobRunnerBuilder(job)

  class JobRunnerBuilder[ParamValues <: HList, Result, Func, PodSpecFunc](
    job: Job[ParamValues, Result, Func, PodSpecFunc]
  ) {
    def apply(func: Directory => Func)(implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
                                       encoderP: Encoder[ParamValues],
                                       decoderP: Decoder[ParamValues],
                                       encoderR: Encoder[Result],
                                       decoderR: Decoder[Result]) =
      JobRunner(job, (_: ParamValues) => PodSpec(Seq.empty), fnToProdFunc(func(Directory("."))))

    def apply(podSpec: PodSpec)(func: Directory => Func)(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      JobRunner(job, (_: ParamValues) => podSpec, fnToProdFunc(func(Directory("."))))

    def apply(podSpecFunc: PodSpecFunc)(func: Directory => Func)(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      JobRunner(job, fnToProdPodSpec(podSpecFunc), fnToProdFunc(func(Directory("."))))
  }
}
