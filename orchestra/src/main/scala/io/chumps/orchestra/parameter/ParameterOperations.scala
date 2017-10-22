package io.chumps.orchestra.parameter

import io.chumps.orchestra.parameter.Parameter.State
import japgolly.scalajs.react.vdom.TagMod
import shapeless._

import io.chumps.orchestra.model.RunId

trait ParameterOperations[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: State): Seq[TagMod]
  def values(params: Params, valueMap: Map[Symbol, Any], runId: RunId): ParamValues
}

object ParameterOperations {

  implicit val hNil = new ParameterOperations[HNil, HNil] {
    override def displays(params: HNil, state: State) = Seq.empty
    override def values(params: HNil, map: Map[Symbol, Any], runId: RunId) = HNil
  }

  implicit def hConsRunId[Params <: HList, TailParamValues <: HList](
    implicit tailParamOperations: ParameterOperations[Params, TailParamValues]
  ) = new ParameterOperations[Params, RunId :: TailParamValues] {
    override def displays(params: Params, state: State) =
      tailParamOperations.displays(params, state)

    override def values(params: Params, valueMap: Map[Symbol, Any], runId: RunId) =
      runId :: tailParamOperations.values(params, valueMap, runId)
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue], TailParams <: HList, HeadParamValue, TailParamValues <: HList](
    implicit tailParamOperations: ParameterOperations[TailParams, TailParamValues]
  ) = new ParameterOperations[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {
    override def displays(params: HeadParam :: TailParams, state: State) =
      params.head.display(state) +: tailParamOperations.displays(params.tail, state)

    override def values(params: HeadParam :: TailParams, valueMap: Map[Symbol, Any], runId: RunId) =
      params.head.getValue(valueMap) :: tailParamOperations.values(params.tail, valueMap, runId)
  }
}
