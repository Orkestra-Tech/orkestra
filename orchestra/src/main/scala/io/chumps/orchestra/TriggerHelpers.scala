package io.chumps.orchestra

import shapeless._
import shapeless.ops.hlist.Tupler
import io.circe.generic.auto._
import io.chumps.orchestra.ARunStatus._
import io.circe.java8.time._

trait TriggerHelpers {

  implicit class TiggerableNoParamJob(job: Job.Runner[HNil, _]) {
    def trigger(): Unit = {
      triggerMessage(job)
      job.ApiServer.trigger(jobRunInfo(job).runId, HNil)
    }

    def triggerAndAwait(): Unit = {
      trigger()
      awaitJobResult(job)
    }
  }

  implicit class TiggerableOneParamJob[ParamValue](job: Job.Runner[ParamValue :: HNil, _]) {
    def trigger(params: ParamValue): Unit = {
      triggerMessage(job)
      job.ApiServer.trigger(jobRunInfo(job).runId, params :: HNil)
    }

    def triggerAndAwait(params: ParamValue): Unit = {
      trigger(params)
      awaitJobResult(job)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList, TupledValues](job: Job.Runner[ParamValues, _])(
    implicit tupler: Tupler.Aux[ParamValues, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValues]
  ) {
    def trigger(params: TupledValues): Unit = {
      triggerMessage(job)
      job.ApiServer.trigger(jobRunInfo(job).runId, tupleToHList.to(params))
    }

    def triggerAndAwait(params: TupledValues): Unit = {
      trigger(params)
      awaitJobResult(job)
    }
  }

  private def triggerMessage(job: Job.Runner[_, _]) = println(s"Triggering ${job.definition.id.name}")

  private def jobRunInfo(job: Job.Runner[_ <: HList, _]) =
    RunInfo(job.definition,
            OrchestraConfig.runInfo.fold(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))(_.runId))

  private def awaitJobResult(job: Job.Runner[_ <: HList, _]): Unit = {
    val runInfo = jobRunInfo(job)
    def isInProgress() = ARunStatus.current(runInfo) match {
      case _: Triggered | _: Running => true
      case _                         => false
    }

    while (isInProgress()) Thread.sleep(500)

    ARunStatus.current(runInfo) match {
      case _: Success    =>
      case Failure(_, e) => throw new IllegalStateException(s"Run of job ${runInfo.job.id.name} failed", e)
      case s             => throw new IllegalStateException(s"Run of job ${runInfo.job.id.name} failed with status $s")
    }
  }
}
