package com.goyeau.orchestra

import shapeless._
import shapeless.ops.hlist.Tupler

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[_, HNil, _, _]) {
    def trigger(job: Job.Runner[_, HNil, _, _]) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), HNil)
    }
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[_, ParamValue :: HNil, _, _]) {
    def trigger(params: ParamValue) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), params :: HNil)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList, TupledValues](job: Job.Runner[_, ParamValues, _, _])(
    implicit tupler: Tupler.Aux[ParamValues, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValues]
  ) {
    def trigger(params: TupledValues) = {
      triggerMessage(job)
      job.apiServer.trigger(runInfo(job), tupleToHList.to(params))
    }
  }

  private def triggerMessage(job: Job.Runner[_, _, _, _]) = println(s"Triggering ${job.definition.id.name}")

  private def runInfo(job: Job.Runner[_, _, _, _]) = RunInfo(job.definition.id, OrchestraConfig.runInfo.map(_.runId))
}
