package com.goyeau.orkestra.utils

import shapeless._

import com.goyeau.orkestra.model.RunId

trait RunIdOperation[ParamValuesNoRunId <: HList, ParamValues <: HList] {
  def inject(valuesNoRunId: ParamValuesNoRunId, runId: RunId): ParamValues
  def remove(values: ParamValues): ParamValuesNoRunId
}

object RunIdOperation {
  implicit val hNil = new RunIdOperation[HNil, HNil] {
    override def inject(valuesNoRunId: HNil, runId: RunId) = HNil
    override def remove(values: HNil) = HNil
  }

  implicit def hConsRunId[ParamValuesNoRunId <: HList, TailParamValues <: HList](
    implicit tailRunIdInjector: RunIdOperation[ParamValuesNoRunId, TailParamValues]
  ) = new RunIdOperation[ParamValuesNoRunId, RunId :: TailParamValues] {

    override def inject(valuesNoRunId: ParamValuesNoRunId, runId: RunId) =
      runId :: tailRunIdInjector.inject(valuesNoRunId, runId)

    override def remove(values: RunId :: TailParamValues) = tailRunIdInjector.remove(values.tail)
  }

  implicit def hCons[HeadParamValue, TailParamValuesNoRunId <: HList, TailParamValues <: HList](
    implicit tailRunIdInjector: RunIdOperation[TailParamValuesNoRunId, TailParamValues],
    ev: HeadParamValue <:!< RunId
  ) = new RunIdOperation[HeadParamValue :: TailParamValuesNoRunId, HeadParamValue :: TailParamValues] {

    override def inject(valuesNoRunId: HeadParamValue :: TailParamValuesNoRunId, runId: RunId) =
      valuesNoRunId.head :: tailRunIdInjector.inject(valuesNoRunId.tail, runId)

    override def remove(values: HeadParamValue :: TailParamValues) =
      values.head :: tailRunIdInjector.remove(values.tail)
  }
}
