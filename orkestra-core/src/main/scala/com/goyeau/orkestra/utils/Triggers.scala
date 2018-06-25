package com.goyeau.orkestra.utils

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

import com.goyeau.orkestra.job.Job
import com.goyeau.orkestra.model.Indexed.{HistoryIndex, Run}
import com.goyeau.orkestra.model.{RunId, RunInfo}
import com.goyeau.orkestra.utils.BaseEncoders._
import com.goyeau.orkestra.utils.AkkaImplicits._
import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.kubernetes.Kubernetes

trait Triggers {
  implicit protected def orkestraConfig: OrkestraConfig
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
      job.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil)

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(): Future[Result] =
      for {
        _ <- job
          .ApiServer()
          .trigger(orkestraConfig.runInfo.runId, HNil, parent = Option(orkestraConfig.runInfo))
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
      job.ApiServer().trigger(orkestraConfig.runInfo.runId, orkestraConfig.runInfo.runId :: HNil)

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
            orkestraConfig.runInfo.runId,
            orkestraConfig.runInfo.runId :: HNil,
            parent = Option(orkestraConfig.runInfo)
          )
        result <- jobResult(job)
      } yield result
  }

  implicit class TriggerableMultipleParamJob[
    ParamValues <: HList: Decoder,
    TupledValues <: Product,
    Result: Decoder
  ](
    job: Job[ParamValues, Result]
  )(
    implicit tupler: Tupler.Aux[ParamValues, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValues]
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
        .trigger(orkestraConfig.runInfo.runId, tupleToHList.to(values))

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(values: TupledValues): Future[Result] =
      for {
        _ <- job
          .ApiServer()
          .trigger(orkestraConfig.runInfo.runId, tupleToHList.to(values), parent = Option(orkestraConfig.runInfo))
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
          HistoryIndex.formatId(RunInfo(job.board.id, orkestraConfig.runInfo.runId))
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
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override lazy val elasticsearchClient: HttpClient = Elasticsearch.client
}
