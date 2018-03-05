package io.chumps.orchestra.utils

import java.io.IOException

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import shapeless._
import shapeless.ops.hlist.Tupler

import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.Indexed.{HistoryIndex, Run}
import io.chumps.orchestra.model.{RunId, RunInfo}
import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.kubernetes.Kubernetes

trait Triggers {
  protected implicit val orchestraConfig: OrchestraConfig
  protected implicit val kubernetesClient: KubernetesClient
  protected implicit val elasticsearchClient: HttpClient

  implicit class TriggerableNoParamJob[Result: Decoder](jobRunner: JobRunner[HNil, Result]) {
    def trigger(): Future[Unit] =
      jobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil)

    def run(): Future[Result] =
      for {
        _ <- jobRunner
          .ApiServer()
          .trigger(orchestraConfig.runInfo.runId, HNil, parent = Option(orchestraConfig.runInfo))
        result <- jobResult(jobRunner)
      } yield result
  }

  implicit class TriggerableRunIdJob[Result: Decoder](jobRunner: JobRunner[RunId :: HNil, Result]) {
    def trigger(): Future[Unit] =
      jobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, orchestraConfig.runInfo.runId :: HNil)

    def run(): Future[Result] =
      for {
        _ <- jobRunner
          .ApiServer()
          .trigger(orchestraConfig.runInfo.runId,
                   orchestraConfig.runInfo.runId :: HNil,
                   parent = Option(orchestraConfig.runInfo))
        result <- jobResult(jobRunner)
      } yield result
  }

  implicit class TriggerableMultipleParamJob[ParamValues <: HList: Decoder,
                                             ParamValuesNoRunId <: HList,
                                             TupledValues <: Product,
                                             Result: Decoder](
    jobRunner: JobRunner[ParamValues, Result]
  )(
    implicit runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
    tupler: Tupler.Aux[ParamValuesNoRunId, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunId]
  ) {
    def trigger(values: TupledValues): Future[Unit] =
      jobRunner
        .ApiServer()
        .trigger(orchestraConfig.runInfo.runId,
                 runIdOperation.inject(tupleToHList.to(values), orchestraConfig.runInfo.runId))

    def run(values: TupledValues): Future[Result] =
      for {
        _ <- jobRunner
          .ApiServer()
          .trigger(orchestraConfig.runInfo.runId,
                   runIdOperation.inject(tupleToHList.to(values), orchestraConfig.runInfo.runId),
                   parent = Option(orchestraConfig.runInfo))
        result <- jobResult(jobRunner)
      } yield result
  }

  private def jobResult[ParamValues <: HList: Decoder, Result: Decoder](
    jobRunner: JobRunner[ParamValues, Result]
  ): Future[Result] =
    for {
      runResponse <- elasticsearchClient.execute(
        get(HistoryIndex.index,
            HistoryIndex.`type`,
            HistoryIndex.formatId(RunInfo(jobRunner.job.id, orchestraConfig.runInfo.runId)))
      )
      run = runResponse
        .fold(failure => throw new IOException(failure.error.reason), identity)
        .result
        .toOpt[Run[ParamValues, Result]]
      result <- run.fold(jobResult(jobRunner))(_.result.fold(jobResult(jobRunner))(_.fold(throw _, Future(_))))
    } yield result
}

object Triggers extends Triggers {
  override implicit val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override val kubernetesClient: KubernetesClient = Kubernetes.client
  override val elasticsearchClient: HttpClient = Elasticsearch.client
}
