package com.goyeau.orchestra.utils

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

import com.goyeau.orchestra.job.Job
import com.goyeau.orchestra.model.Indexed.{HistoryIndex, Run}
import com.goyeau.orchestra.model.{RunId, RunInfo}
import com.goyeau.orchestra.utils.BaseEncoders._
import com.goyeau.orchestra.utils.AkkaImplicits._
import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.orchestra.kubernetes.Kubernetes

trait Triggers {
  implicit protected def orchestraConfig: OrchestraConfig
  implicit protected def kubernetesClient: KubernetesClient
  implicit protected def elasticsearchClient: HttpClient

  implicit class TriggerableNoParamJob[Result: Decoder](job: Job[HNil, Result]) {

    /**
      * Trigger the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * This is a fire and forget action. If you want the result of the job or await the completion of the job see
      * run().
      */
    def trigger(): Future[Unit] =
      job.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil)

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(): Future[Result] =
      for {
        _ <- job
          .ApiServer()
          .trigger(orchestraConfig.runInfo.runId, HNil, parent = Option(orchestraConfig.runInfo))
        result <- jobResult(job)
      } yield result
  }

  implicit class TriggerableRunIdJob[Result: Decoder](job: Job[RunId :: HNil, Result]) {

    /**
      * Trigger the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * This is a fire and forget action. If you want the result of the job or await the completion of the job see
      * run().
      */
    def trigger(): Future[Unit] =
      job.ApiServer().trigger(orchestraConfig.runInfo.runId, orchestraConfig.runInfo.runId :: HNil)

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(): Future[Result] =
      for {
        _ <- job
          .ApiServer()
          .trigger(
            orchestraConfig.runInfo.runId,
            orchestraConfig.runInfo.runId :: HNil,
            parent = Option(orchestraConfig.runInfo)
          )
        result <- jobResult(job)
      } yield result
  }

  implicit class TriggerableMultipleParamJob[
    ParamValues <: HList: Decoder,
    ParamValuesNoRunId <: HList,
    TupledValues <: Product,
    Result: Decoder
  ](
    job: Job[ParamValues, Result]
  )(
    implicit runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
    tupler: Tupler.Aux[ParamValuesNoRunId, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunId]
  ) {

    /**
      * Trigger the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * This is a fire and forget action. If you want the result of the job or await the completion of the job see
      * run().
      */
    def trigger(values: TupledValues): Future[Unit] =
      job
        .ApiServer()
        .trigger(
          orchestraConfig.runInfo.runId,
          runIdOperation.inject(tupleToHList.to(values), orchestraConfig.runInfo.runId)
        )

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(values: TupledValues): Future[Result] =
      for {
        _ <- job
          .ApiServer()
          .trigger(
            orchestraConfig.runInfo.runId,
            runIdOperation.inject(tupleToHList.to(values), orchestraConfig.runInfo.runId),
            parent = Option(orchestraConfig.runInfo)
          )
        result <- jobResult(job)
      } yield result
  }

  private def jobResult[ParamValues <: HList: Decoder, Result: Decoder](
    job: Job[ParamValues, Result]
  ): Future[Result] =
    for {
      runResponse <- elasticsearchClient.execute(
        get(
          HistoryIndex.index,
          HistoryIndex.`type`,
          HistoryIndex.formatId(RunInfo(job.board.id, orchestraConfig.runInfo.runId))
        )
      )
      run = runResponse
        .fold(failure => throw new IOException(failure.error.reason), identity)
        .result
        .toOpt[Run[ParamValues, Result]]
      result <- run.fold(jobResult(job))(_.result.fold(jobResult(job))(_.fold(throw _, Future(_))))
    } yield result
}

object Triggers extends Triggers {
  implicit override lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override lazy val elasticsearchClient: HttpClient = Elasticsearch.client
}
