package tech.orkestra.cron

import cats.effect.Effect
import io.k8s.api.core.v1.PodSpec
import shapeless._
import tech.orkestra.job.Job
import tech.orkestra.model.JobId

/**
  * A cron triggerable job.
  *
  * @param schedule The cron schedule expression
  * @param job The job to trigger
  */
case class CronTrigger[F[_]: Effect, Parameters <: HList](
  schedule: String,
  job: Job[F, Parameters, _],
  parameters: Parameters
) {
  val podSpecWithDefaultParams: PodSpec = job.podSpec(parameters)
  val jobId: JobId = job.board.id
}
