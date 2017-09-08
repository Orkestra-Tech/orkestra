package com.goyeau.orchestra

import shapeless._
import shapeless.ops.hlist.Tupler

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[HNil, _]) {
    def trigger(job: Job.Runner[HNil, _]) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), HNil)
    }
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[ParamValue :: HNil, _]) {
    def trigger(params: ParamValue) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), params :: HNil)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList, TupledValues](job: Job.Runner[ParamValues, _])(
    implicit tupler: Tupler.Aux[ParamValues, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValues]
  ) {
    def trigger(params: TupledValues) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), tupleToHList.to(params))
    }
  }

  private def triggerMessage(job: Job.Runner[_, _]) = println(s"Triggering ${job.definition.id.name}")

  private def runInfo(job: Job.Runner[_, _]) = RunInfo(job.definition.id, OrchestraConfig.runInfo.map(_.runId))
}
