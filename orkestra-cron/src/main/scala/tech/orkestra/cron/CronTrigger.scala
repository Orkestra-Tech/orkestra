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
trait CronTrigger {
  def schedule: String
  private[cron] def jobId: JobId
  private[cron] def podSpecWithDefaultParams: PodSpec
}

object CronTrigger {
  def apply[F[_]: Effect, Parameters <: HList](
    schedule: String,
    job: Job[F, Parameters, _],
    parameters: Parameters
  ): CronTrigger = CronJobTrigger(schedule, job, parameters)
}

case class CronJobTrigger[F[_]: Effect, Parameters <: HList](
  schedule: String,
  job: Job[F, Parameters, _],
  parameters: Parameters
) extends CronTrigger {
  val podSpecWithDefaultParams = job.podSpec(parameters)
  val jobId: JobId = job.board.id
}
