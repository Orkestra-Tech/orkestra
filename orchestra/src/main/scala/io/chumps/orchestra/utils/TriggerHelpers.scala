package io.chumps.orchestra.utils

import io.circe.Decoder
import shapeless._
import shapeless.ops.hlist.Tupler
import scala.concurrent.duration._

import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.{RunId, RunInfo}
import io.chumps.orchestra.{ARunStatus, OrchestraConfig}

trait TriggerHelpers {

  implicit class TriggerableNoParamJob[Result: Decoder](jobRunner: JobRunner[HNil, Result]) {
    def trigger(): Unit =
      jobRunner.ApiServer.trigger(jobRunInfo(jobRunner).runId, HNil)

    def run(): Result = {
      trigger()
      awaitJobResult(jobRunner)
    }
  }

  implicit class TriggerableRunIdJob[Result: Decoder](jobRunner: JobRunner[RunId :: HNil, Result]) {
    def trigger(): Unit = {
      val runId = jobRunInfo(jobRunner).runId
      jobRunner.ApiServer.trigger(runId, runId :: HNil)
    }

    def run(): Result = {
      trigger()
      awaitJobResult(jobRunner)
    }
  }

  implicit class TriggerableMultipleParamJob[ParamValues <: HList,
                                             ParamValuesNoRunId <: HList,
                                             TupledValues <: Product,
                                             Result: Decoder](
    jobRunner: JobRunner[ParamValues, Result]
  )(
    implicit runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
    tupler: Tupler.Aux[ParamValuesNoRunId, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunId]
  ) {
    def trigger(values: TupledValues): Unit = {
      val runId = jobRunInfo(jobRunner).runId
      jobRunner.ApiServer.trigger(runId, runIdOperation.inject(tupleToHList.to(values), runId))
    }

    def run(params: TupledValues): Result = {
      trigger(params)
      awaitJobResult(jobRunner)
    }
  }

  private def jobRunInfo(jobRunner: JobRunner[_ <: HList, _]) =
    RunInfo(jobRunner.job.id, OrchestraConfig.runInfo.runId)

  private def awaitJobResult[Result: Decoder](jobRunner: JobRunner[_ <: HList, Result]): Result = {
    val runInfo = jobRunInfo(jobRunner)
    def isChildJobInProgress() = ARunStatus.current[Result](runInfo, checkRunning = false) match {
      case _: Triggered | _: Running => true
      case _                         => false
    }

    while (isChildJobInProgress()) {
      ARunStatus.current[Result](OrchestraConfig.runInfo) match {
        case _: Stopped => jobRunner.ApiServer.stop(runInfo.runId)
        case _          =>
      }
      Thread.sleep(0.5.second.toMillis)
    }

    ARunStatus.current[Result](runInfo) match {
      case Success(_, result) => result
      case Failure(_, e)      => throw new IllegalStateException(s"Run of job ${jobRunner.job.name} failed", e)
      case s                  => throw new IllegalStateException(s"Run of job ${jobRunner.job.name} failed with status $s")
    }
  }
}
