package tech.orkestra.cron

import shapeless._
import tech.orkestra.job.Job

/**
  * A cron triggerable job.
  *
  * @param schedule The cron schedule expression
  * @param job The job to trigger
  */
case class CronTrigger[ParamValues <: HList] private (
  schedule: String,
  job: Job[ParamValues, _],
  paramsValues: ParamValues
) {
  private[cron] val podSpecWithDefaultParams = job.podSpec(paramsValues)
}

object CronTrigger {
  def apply[ParamValues <: HList](schedule: String, job: Job[ParamValues, _]) =
    new CronTriggerBuilder[ParamValues](schedule, job)

  class CronTriggerBuilder[ParamValues <: HList](repository: String, job: Job[ParamValues, _]) {
    // No Param
    def apply()(implicit defaultParamsWitness: ParamValuesWitness[HNil, ParamValues]): CronTrigger[ParamValues] =
      CronTrigger(repository, job, defaultParamsWitness(HNil))

    // One param
    def apply[ParamValue](
      value: ParamValue
    )(implicit defaultParamsWitness: ParamValuesWitness[ParamValue :: HNil, ParamValues]): CronTrigger[ParamValues] =
      CronTrigger(repository, job, defaultParamsWitness(value :: HNil))

    // Multi param
    def apply[TupledValues <: Product](paramValues: TupledValues)(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValues]
    ): CronTrigger[ParamValues] =
      CronTrigger(repository, job, tupleToHList.to(paramValues))
  }
}
