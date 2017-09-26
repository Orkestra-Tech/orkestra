package com.goyeau.orchestra

import shapeless._
import shapeless.ops.hlist.Tupler
import io.circe.generic.auto._
import com.goyeau.orchestra.ARunStatus._
import io.circe.java8.time._

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[HNil, _, _]) {
    def run() = {
      triggerMessage(job)
      val runInfo = jobRunInfo(job)
      job.apiServer.trigger(runInfo, HNil)
      awaitJobResult(runInfo)
    }
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[ParamValue :: HNil, _, _]) {
    def run(params: ParamValue) = {
      triggerMessage(job)
      val runInfo = jobRunInfo(job)
      job.apiServer.trigger(runInfo, params :: HNil)
      awaitJobResult(runInfo)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList, TupledValues](job: Job.Runner[ParamValues, _, _])(
    implicit tupler: Tupler.Aux[ParamValues, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValues]
  ) {
    def run(params: TupledValues) = {
      triggerMessage(job)
      val runInfo = jobRunInfo(job)
      job.apiServer.trigger(runInfo, tupleToHList.to(params))
      awaitJobResult(runInfo)
    }
  }

  private def triggerMessage(job: Job.Runner[_, _, _]) = println(s"Triggering ${job.definition.id.name}")

  private def jobRunInfo(job: Job.Runner[_, _, _]) = RunInfo(job.definition.id, OrchestraConfig.runInfo.map(_.runId))

  private def awaitJobResult(runInfo: RunInfo) = {
    def isInProgress() = RunStatusUtils.current(runInfo) match {
      case _: Triggered => true
      case _: Running   => true
      case _            => false
    }

    while (isInProgress()) Thread.sleep(500)

    RunStatusUtils.current(runInfo) match {
      case _: Success    =>
      case Failure(_, e) => throw new IllegalStateException(s"Run of job ${runInfo.jobId.name} failed", e)
      case s             => throw new IllegalStateException(s"Run of job ${runInfo.jobId.name} failed with status $s")
    }
  }
}
