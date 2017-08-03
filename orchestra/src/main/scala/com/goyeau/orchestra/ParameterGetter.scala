package com.goyeau.orchestra

import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil}

trait ParameterGetter[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: ParameterDisplayer.State): Seq[TagMod]
  def values(params: Params, valueMap: Map[Symbol, Any]): ParamValues
}

object ParameterGetter {

  implicit val hNil = new ParameterGetter[HNil, HNil] {
    override def displays(params: HNil, state: ParameterDisplayer.State) = Seq.empty
    override def values(params: HNil, map: Map[Symbol, Any]) = HNil
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue], TailParams <: HList, HeadParamValue, TailParamValues <: HList](
    implicit displayer: ParameterDisplayer[HeadParam],
    paramGetter: ParameterGetter[TailParams, TailParamValues]
  ) = new ParameterGetter[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {
    override def displays(params: HeadParam :: TailParams, state: ParameterDisplayer.State) =
      displayer(params.head, state) +: paramGetter.displays(params.tail, state)

    override def values(params: HeadParam :: TailParams, valueMap: Map[Symbol, Any]) =
      params.head.getValue(valueMap) :: paramGetter.values(params.tail, valueMap)
  }
}
