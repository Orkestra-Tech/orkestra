package tech.orkestra.utils

import java.io.IOException

import cats.Applicative
import cats.effect._
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

import scala.concurrent.ExecutionContext

trait Triggers[F[_]] {
  implicit protected def F: ConcurrentEffect[F]
  implicit protected def orkestraConfig: OrkestraConfig
  protected def kubernetesClient: Resource[F, KubernetesClient[F]]
  implicit protected def elasticsearchClient: ElasticClient

  implicit class TriggerableMultipleParamJob[Parameters <: HList: Decoder, Result: Decoder](
    job: Job[F, Parameters, Result]
  ) {

    /**
      * Trigger the job with the same run id as the current job. This means the triggered job will output in the same
      * log as the triggering job.
      * This is a fire and forget action. If you want the result of the job or await the completion of the job see
      * run().
      */
    def trigger(parameters: Parameters): F[Unit] = kubernetesClient.use { implicit kubernetesClient =>
      IO.fromFuture(IO(job.ApiServer().trigger(orkestraConfig.runInfo.runId, parameters))).to[F]
    }

    /**
      * Run the job with the same run id as the current job. This means the triggered job will output in the same log
      * as the triggering job.
      * It returns a Future with the result of the job ran.
      */
    def run(parameters: Parameters): F[Result] = kubernetesClient.use { implicit kubernetesClient =>
      IO.fromFuture(IO {
          job.ApiServer().trigger(orkestraConfig.runInfo.runId, parameters, parent = Option(orkestraConfig.runInfo))
        })
        .to[F] *>
        jobResult(job)
    }
  }

  private def jobResult[Parameters <: HList: Decoder, Result: Decoder](job: Job[F, Parameters, Result]): F[Result] =
    for {
      response <- IO
        .fromFuture(IO {
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

object Triggers extends Triggers[IO] {
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val kubernetesClient: Resource[IO, KubernetesClient[IO]] = Kubernetes.client
  override lazy val elasticsearchClient: ElasticClient = Elasticsearch.client
}
