package io.chumps.orchestra.parameter

import japgolly.scalajs.react.vdom.TagMod
import shapeless._

trait ParameterOperations[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: State): Seq[TagMod]
  def values(params: Params, valueMap: Map[Symbol, Any]): ParamValues
  def paramsState(params: Params, paramValues: ParamValues): Map[String, Any]
}

object ParameterOperations {

  implicit val hNil = new ParameterOperations[HNil, HNil] {
    override def displays(params: HNil, state: State) = Seq.empty
    override def values(params: HNil, valueMap: Map[Symbol, Any]) = HNil
    override def paramsState(params: HNil, paramValues: HNil) = Map.empty
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue],
                     TailParams <: HList,
                     HeadParamValue,
                     TailParamValues <: HList](
    implicit tailParamOperations: ParameterOperations[TailParams, TailParamValues]
  ) = new ParameterOperations[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {

    override def displays(params: HeadParam :: TailParams, state: State) =
      params.head.display(state) +: tailParamOperations.displays(params.tail, state)

    override def values(params: HeadParam :: TailParams, valueMap: Map[Symbol, Any]) =
      params.head.getValue(valueMap) :: tailParamOperations.values(params.tail, valueMap)

    override def paramsState(params: HeadParam :: TailParams, paramValues: HeadParamValue :: TailParamValues) =
      tailParamOperations.paramsState(params.tail, paramValues.tail) + (params.head.name -> paramValues.head)
  }
}
