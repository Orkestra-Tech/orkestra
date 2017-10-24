package io.chumps.orchestra.parameter

import io.chumps.orchestra.parameter.Parameter.State
import japgolly.scalajs.react.vdom.TagMod
import shapeless._

import io.chumps.orchestra.model.RunId

trait ParameterOperations[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: State): Seq[TagMod]
  def values(params: Params, valueMap: Map[Symbol, Any], runId: RunId): ParamValues
  def paramsState(params: Params, paramValues: ParamValues): Map[String, Any]
}

object ParameterOperations {

  implicit val hNil = new ParameterOperations[HNil, HNil] {
    override def displays(params: HNil, state: State) = Seq.empty
    override def values(params: HNil, map: Map[Symbol, Any], runId: RunId) = HNil
    def paramsState(params: HNil, paramValues: HNil) = Map.empty
  }

  implicit def hConsRunId[Params <: HList, TailParamValues <: HList](
    implicit tailParamOperations: ParameterOperations[Params, TailParamValues]
  ) = new ParameterOperations[Params, RunId :: TailParamValues] {
    override def displays(params: Params, state: State) =
      tailParamOperations.displays(params, state)

    override def values(params: Params, valueMap: Map[Symbol, Any], runId: RunId) =
      runId :: tailParamOperations.values(params, valueMap, runId)

    override def paramsState(params: Params, paramValues: RunId :: TailParamValues) =
      tailParamOperations.paramsState(params, paramValues.tail)
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue], TailParams <: HList, HeadParamValue, TailParamValues <: HList](
    implicit tailParamOperations: ParameterOperations[TailParams, TailParamValues]
  ) = new ParameterOperations[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {
    override def displays(params: HeadParam :: TailParams, state: State) =
      params.head.display(state) +: tailParamOperations.displays(params.tail, state)

    override def values(params: HeadParam :: TailParams, valueMap: Map[Symbol, Any], runId: RunId) =
      params.head.getValue(valueMap) :: tailParamOperations.values(params.tail, valueMap, runId)

    override def paramsState(params: HeadParam :: TailParams, paramValues: HeadParamValue :: TailParamValues) =
      tailParamOperations.paramsState(params.tail, paramValues.tail) + (params.head.name -> paramValues.head)
  }
}
