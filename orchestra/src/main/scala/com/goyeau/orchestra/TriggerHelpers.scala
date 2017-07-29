package com.goyeau.orchestra

import RunInfo
import shapeless._

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[_, HNil, _, _]) {
    def trigger(job: Job.Runner[_, HNil, _, _]) =
      job.apiServer.run(runInfo(job), HNil)
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[_, ParamValue :: HNil, _, _]) {
    def trigger(params: ParamValue) =
      job.apiServer.run(runInfo(job), params :: HNil)
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList](job: Job.Runner[_, ParamValues, _, _]) {
    def trigger[TupledValues](params: TupledValues)(implicit tupleToHList: Generic.Aux[TupledValues, ParamValues]) =
      job.apiServer.run(runInfo(job), tupleToHList.to(params))
  }

  private def runInfo(job: Job.Runner[_, _, _, _]) = RunInfo(job.definition.id, Config.runInfo.map(_.runId))
}
