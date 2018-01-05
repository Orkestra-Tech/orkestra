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
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId, HNil)

    def run(): Result = {
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId, HNil, by = Option(OrchestraConfig.runInfo))
      awaitJobResult(jobRunner)
    }
  }

  implicit class TriggerableRunIdJob[Result: Decoder](jobRunner: JobRunner[RunId :: HNil, Result]) {
    def trigger(): Unit =
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId, OrchestraConfig.runInfo.runId :: HNil)

    def run(): Result = {
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId,
                                  OrchestraConfig.runInfo.runId :: HNil,
                                  by = Option(OrchestraConfig.runInfo))
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
    def trigger(values: TupledValues): Unit =
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId,
                                  runIdOperation.inject(tupleToHList.to(values), OrchestraConfig.runInfo.runId))

    def run(values: TupledValues): Result = {
      jobRunner.ApiServer.trigger(OrchestraConfig.runInfo.runId,
                                  runIdOperation.inject(tupleToHList.to(values), OrchestraConfig.runInfo.runId),
                                  by = Option(OrchestraConfig.runInfo))
      awaitJobResult(jobRunner)
    }
  }

  private def awaitJobResult[Result: Decoder](jobRunner: JobRunner[_ <: HList, Result]): Result = {
    val runInfo = RunInfo(jobRunner.job.id, OrchestraConfig.runInfo.runId)
    def isChildJobInProgress() = ARunStatus.current[Result](runInfo, checkRunning = false) match {
      case Some(Triggered(_, _) | Running(_)) => true
      case _                                  => false
    }

    while (isChildJobInProgress()) Thread.sleep(0.5.second.toMillis)

    ARunStatus.current[Result](runInfo) match {
      case None                     => throw new IllegalStateException(s"No status found for job ${runInfo.jobId} ${runInfo.runId}")
      case Some(Success(_, result)) => result
      case Some(Failure(_, e))      => throw new IllegalStateException(s"Run of job ${jobRunner.job.name} failed", e)
      case s                        => throw new IllegalStateException(s"Run of job ${jobRunner.job.name} failed with status $s")
    }
  }
}
