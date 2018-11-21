package tech.orkestra.utils

import java.io.IOException

import cats.Applicative
import cats.effect.{Async, IO, Sync}
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.ElasticClient
import io.circe.Decoder
import shapeless._
import tech.orkestra.job.Job
import tech.orkestra.model.Indexed.{HistoryIndex, Run}
import tech.orkestra.model.RunInfo
import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.OrkestraConfig
import tech.orkestra.kubernetes.Kubernetes

trait Triggers {
  implicit protected def orkestraConfig: OrkestraConfig
  implicit protected def kubernetesClient: KubernetesClient
  implicit protected def elasticsearchClient: ElasticClient

  implicit class TriggerableMultipleParamJob[
    F[_]: Async,
    Parameters <: HList: Decoder,
    Result: Decoder
  ](job: Job[F, Parameters, Result]) {

    /**
      * Trigger the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * This is a fire and forget action. If you want the result of the job or await the completion of the job see
      * run().
      */
    def trigger(parameters: Parameters): F[Unit] =
      IO.fromFuture(IO {
        job.ApiServer().trigger(orkestraConfig.runInfo.runId, parameters)
      })
        .to[F]

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same log
      * as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(parameters: Parameters): F[Result] =
      for {
        _ <- IO.fromFuture(IO {
          job.ApiServer().trigger(orkestraConfig.runInfo.runId, parameters, parent = Option(orkestraConfig.runInfo))
        })
          .to[F]
        result <- jobResult(job)
      } yield result
  }

  private def jobResult[F[_]: Async, Parameters <: HList: Decoder, Result: Decoder](
    job: Job[F, Parameters, Result]
  ): F[Result] =
    for {
      response <- IO.fromFuture(IO {
        elasticsearchClient.execute(
          get(
            HistoryIndex.index,
            HistoryIndex.`type`,
            HistoryIndex.formatId(RunInfo(job.board.id, orkestraConfig.runInfo.runId))
          )
        )
      })
        .to[F]
      run = response.fold(throw new IOException(response.error.reason))(_.toOpt[Run[Parameters, Result]])
      result <- run.fold(jobResult(job))(
        _.result.fold(jobResult(job))(_.fold(Sync[F].raiseError, Applicative[F].pure(_)))
      )
    } yield result
}

object Triggers extends Triggers {
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override lazy val elasticsearchClient: ElasticClient = Elasticsearch.client
}
