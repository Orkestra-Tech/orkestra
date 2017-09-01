package com.goyeau.orchestra

import shapeless._

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[_, HNil, _, _]) {
    def trigger(job: Job.Runner[_, HNil, _, _]) = {
      triggerMessage(job)
      job.apiServer.run(runInfo(job), HNil)
    }
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[_, ParamValue :: HNil, _, _]) {
    def trigger(params: ParamValue) = {
      triggerMessage(job)
      job.apiServer.run(runInfo(job), params :: HNil)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList](job: Job.Runner[_, ParamValues, _, _]) {
    def trigger[TupledValues](params: TupledValues)(implicit tupleToHList: Generic.Aux[TupledValues, ParamValues]) = {
      triggerMessage(job)
      job.apiServer.run(runInfo(job), tupleToHList.to(params))
    }
  }

  private def triggerMessage(job: Job.Runner[_, _, _, _]) = println(s"Triggering ${job.definition.id.name}")

  private def runInfo(job: Job.Runner[_, _, _, _]) = RunInfo(job.definition.id, OrchestraConfig.runInfo.map(_.runId))
}
