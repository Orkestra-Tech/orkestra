package com.goyeau.orchestra

import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil}

trait ParameterOperations[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: ParameterDisplayer.State): Seq[TagMod]
  def values(params: Params, valueMap: Map[Symbol, Any]): ParamValues
}

object ParameterOperations {

  implicit val hNil = new ParameterOperations[HNil, HNil] {
    override def displays(params: HNil, state: ParameterDisplayer.State) = Seq.empty
    override def values(params: HNil, map: Map[Symbol, Any]) = HNil
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue], TailParams <: HList, HeadParamValue, TailParamValues <: HList](
    implicit displayer: ParameterDisplayer[HeadParam],
    value: ParameterGetter[HeadParamValue, HeadParam],
    paramGetter: ParameterOperations[TailParams, TailParamValues]
  ) = new ParameterOperations[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {
    override def displays(params: HeadParam :: TailParams, state: ParameterDisplayer.State) =
      displayer(params.head, state) +: paramGetter.displays(params.tail, state)

    override def values(params: HeadParam :: TailParams, valueMap: Map[Symbol, Any]) =
      value(params.head, valueMap) :: paramGetter.values(params.tail, valueMap)
  }
}
