package io.chumps.orchestra

import shapeless._
import shapeless.ops.hlist.Tupler

import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.model.{RunId, RunInfo}

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

  implicit class TiggerableRunIdJob(job: Job.Runner[RunId :: HNil, _]) {
    def trigger(): Unit = {
      triggerMessage(job)
      val runId = jobRunInfo(job).runId
      job.ApiServer.trigger(runId, runId :: HNil)
    }

    def triggerAndAwait(): Unit = {
      trigger()
      awaitJobResult(job)
    }
  }

  implicit class TiggerableMultipleParamJob[ParamValues <: HList, ParamValuesNoRunId <: HList, TupledValues](
    job: Job.Runner[ParamValues, _]
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
