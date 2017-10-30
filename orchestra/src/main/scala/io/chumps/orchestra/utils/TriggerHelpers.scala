package io.chumps.orchestra.utils

import io.circe.Decoder
import shapeless._
import shapeless.ops.hlist.Tupler

import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.{RunId, RunInfo}
import io.chumps.orchestra.{ARunStatus, OrchestraConfig}

trait TriggerHelpers {

  implicit class TriggerableNoParamJob[Result: Decoder](job: JobRunner[HNil, Result]) {
    def trigger(): Unit = {
      triggerMessage(job)
      job.ApiServer.trigger(jobRunInfo(job).runId, HNil)
    }

    def triggerAndAwait(): Result = {
      trigger()
      awaitJobResult(job)
    }
  }

  implicit class TriggerableRunIdJob[Result: Decoder](job: JobRunner[RunId :: HNil, Result]) {
    def trigger(): Unit = {
      triggerMessage(job)
      val runId = jobRunInfo(job).runId
      job.ApiServer.trigger(runId, runId :: HNil)
    }

    def triggerAndAwait(): Result = {
      trigger()
      awaitJobResult(job)
    }
  }

  implicit class TriggerableMultipleParamJob[ParamValues <: HList,
                                             ParamValuesNoRunId <: HList,
                                             TupledValues,
                                             Result: Decoder](
    job: JobRunner[ParamValues, Result]
  )(
    implicit runIdInjector: RunIdInjector[ParamValuesNoRunId, ParamValues],
    tupler: Tupler.Aux[ParamValuesNoRunId, TupledValues],
    tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunId]
  ) {
    def trigger(params: TupledValues): Unit = {
      triggerMessage(job)
      val runId = jobRunInfo(job).runId
      job.ApiServer.trigger(runId, runIdInjector(tupleToHList.to(params), runId))
    }

    def triggerAndAwait(params: TupledValues): Result = {
      trigger(params)
      awaitJobResult(job)
    }
  }

  private def triggerMessage(jobRunner: JobRunner[_, _]) = println(s"Triggering ${jobRunner.job.id.name}")

  private def jobRunInfo(jobRunner: JobRunner[_ <: HList, _]) =
    RunInfo(jobRunner.job.id,
            OrchestraConfig.runInfo.fold(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))(_.runId))

  private def awaitJobResult[Result: Decoder](job: JobRunner[_ <: HList, Result]): Result = {
    val runInfo = jobRunInfo(job)
    def isInProgress() = ARunStatus.current(runInfo) match {
      case _: Triggered[Result] | _: Running[Result] => true
      case _                                         => false
    }

    while (isInProgress()) Thread.sleep(500)

    ARunStatus.current[Result](runInfo) match {
      case Success(_, result) => result
      case Failure(_, e)      => throw new IllegalStateException(s"Run of job ${runInfo.jobId.name} failed", e)
      case s                  => throw new IllegalStateException(s"Run of job ${runInfo.jobId.name} failed with status $s")
    }
  }
}

trait RunIdInjector[ParamValuesNoRunId <: HList, ParamValues <: HList] {
  def apply(params: ParamValuesNoRunId, runId: RunId): ParamValues
}

object RunIdInjector {
  implicit val hNil = new RunIdInjector[HNil, HNil] {
    override def apply(params: HNil, runId: RunId) = HNil
  }

  implicit def hConsRunId[ParamValuesNoRunId <: HList, TailParamValues <: HList](
    implicit tailParamOperations: RunIdInjector[ParamValuesNoRunId, TailParamValues]
  ) = new RunIdInjector[ParamValuesNoRunId, RunId :: TailParamValues] {

    override def apply(valuesNoRunId: ParamValuesNoRunId, runId: RunId) =
      runId :: tailParamOperations(valuesNoRunId, runId)
  }

  implicit def hCons[HeadParamValue, TailParamValuesNoRunId <: HList, TailParamValues <: HList](
    implicit tailParamOperations: RunIdInjector[TailParamValuesNoRunId, TailParamValues],
    ev: HeadParamValue <:!< RunId
  ) = new RunIdInjector[HeadParamValue :: TailParamValuesNoRunId, HeadParamValue :: TailParamValues] {

    override def apply(params: HeadParamValue :: TailParamValuesNoRunId, runId: RunId) =
      params.head :: tailParamOperations(params.tail, runId)
  }
}
