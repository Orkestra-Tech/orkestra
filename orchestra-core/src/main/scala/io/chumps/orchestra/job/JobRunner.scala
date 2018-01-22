package io.chumps.orchestra.job

import java.io.IOException
import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import io.k8s.api.core.v1.PodSpec
import shapeless._
import shapeless.ops.function.FnToProduct
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.model.Indexed.StagesIndex
import io.chumps.orchestra.model.Indexed.HistoryIndex
import io.chumps.orchestra.model.Indexed.Run
import io.chumps.orchestra.model.Indexed.Stage
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.filesystem.Directory
import io.chumps.orchestra.kubernetes.JobUtils
import io.chumps.orchestra.model._
import io.chumps.orchestra.utils.Utils
import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.{AutowireServer, CommonApiServer, Elasticsearch}

case class JobRunner[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](
  job: Job[ParamValues, Result, _, _],
  podSpec: ParamValues => PodSpec,
  func: ParamValues => Result
) {

  private[orchestra] def run(runInfo: RunInfo): Result = Utils.elasticsearchOutErr(runInfo.runId) {
    Await.result(
      (for {
        getRunResponse <- Elasticsearch.client.execute(
          get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
        )
        run = getRunResponse.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Run]

        _ = system.scheduler.schedule(0.second, 1.second) {
          Elasticsearch.client
            .execute(
              updateById(
                HistoryIndex.index.name + "/" + HistoryIndex.`type`, // TODO: Remove workaround when fixed in elastic4s
                HistoryIndex.`type`,
                HistoryIndex.formatId(runInfo)
              ).doc(Json.obj("latestUpdateOn" -> Instant.now().asJson))
            )
            .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
        }

        _ = run.parentJob.map { parentJob =>
          system.scheduler.schedule(1.second, 1.second) {
            if (!CommonApiServer.runningJobs().contains(parentJob))
              failJob(runInfo, new InterruptedException(s"Parent job ${parentJob.jobId.value} stopped"))
          }
        }

        _ = println(s"Running job ${job.name}")
        result = func(decode[ParamValues](run.paramValues).fold(throw _, identity))
        _ = println(s"Job ${job.name} completed")

        updateResultResponse <- Elasticsearch.client.execute(
          updateById(
            HistoryIndex.index.name + "/" + HistoryIndex.`type`, // TODO: Remove workaround when fixed in elastic4s
            HistoryIndex.`type`,
            HistoryIndex.formatId(runInfo)
          ).doc(Json.obj("result" -> Option(Right(result.asJson.noSpaces): Either[Throwable, String]).asJson))
            .retryOnConflict(1)
        )
        _ = updateResultResponse.fold(failure => throw new IOException(failure.error.reason), identity)
      } yield result)
        .recoverWith { case t => failJob(runInfo, t) }
        .transformWith { tried =>
          JobUtils.delete(runInfo)
          Future.fromTry(tried)
        },
      Duration.Inf
    )
  }

  private[orchestra] object ApiServer extends job.Api {
    override def trigger(runId: RunId,
                         paramValues: ParamValues,
                         tags: Seq[String] = Seq.empty,
                         parent: Option[RunInfo] = None): Unit = {
      val runInfo = RunInfo(job.id, runId)
      Await.result(
        (for {
          indexResponse <- Elasticsearch.client.execute(
            indexInto(HistoryIndex.index, HistoryIndex.`type`)
              .id(HistoryIndex.formatId(runInfo))
              .createOnly(true)
              .source(Run(runInfo, paramValues.asJson.noSpaces, Instant.now(), parent, Instant.now(), None, tags))
          )
          _ = indexResponse.fold(failure => throw new IOException(failure.error.reason), identity)
          _ <- JobUtils.create(runInfo, podSpec(paramValues))
        } yield ()).recoverWith { case t => failJob(runInfo, t) },
        1.minute
      )
    }

    override def stop(runId: RunId): Unit = JobUtils.delete(RunInfo(job.id, runId))

    override def tags(): Seq[String] = {
      val aggregationName = "tagsForJob"
      Await.result(
        Elasticsearch.client
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
              .map(_.key)),
        1.minute
      )
    }

    override def history(page: Page[Instant]): Seq[(Run, Seq[Stage])] = Await.result(
      for {
        runs <- Elasticsearch.client
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
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Run])

        stages <- if (runs.nonEmpty)
          Elasticsearch.client
            .execute(
              search(StagesIndex.index)
                .query(boolQuery.filter(termsQuery("runId", runs.map(_.runInfo.runId.value))))
                .size(1000)
            )
            .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Stage])
        else Future.successful(Seq.empty)
      } yield runs.map(run => (run, stages.filter(_.runId == run.runInfo.runId).sortBy(_.startedOn))),
      1.minute
    )
  }

  private def failJob(runInfo: RunInfo, t: Throwable) = {
    t.printStackTrace()
    Elasticsearch.client
      .execute(
        updateById(
          HistoryIndex.index.name + "/" + HistoryIndex.`type`, // TODO: Remove workaround when fixed in elastic4s
          HistoryIndex.`type`,
          HistoryIndex.formatId(runInfo)
        ).doc(Json.obj("result" -> Option(Left(t): Either[Throwable, String]).asJson))
          .retryOnConflict(1)
      )
      .flatMap(_ => Future.failed(t))
  }

  private[orchestra] val apiRoute: Route = {
    import akka.http.scaladsl.server.Directives._
    path(job.id.value / Segments) { segments =>
      entity(as[String]) { entity =>
        val body = AutowireServer.read[Map[String, String]](entity)
        val request = job.Api.router(ApiServer).apply(Core.Request(segments, body))
        onSuccess(request)(complete(_))
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
