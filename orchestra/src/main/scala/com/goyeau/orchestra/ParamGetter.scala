package com.goyeau.orchestra

import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil}

trait ParamGetter[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: Displayer.State): TagMod
  def values(params: Params, valueMap: Map[String, Any]): ParamValues
}

object ParamGetter {

  implicit val hNil = new ParamGetter[HNil, HNil] {
    override def displays(params: HNil, state: Displayer.State): TagMod = TagMod()
    override def values(params: HNil, map: Map[String, Any]) = HNil
  }

  implicit def hCons[HeadParam <: Parameter[HeadParamValue], TailParams <: HList, HeadParamValue, TailParamValues <: HList](
    implicit displayer: Displayer[HeadParam],
    paramGetter: ParamGetter[TailParams, TailParamValues]
  ) = new ParamGetter[HeadParam :: TailParams, HeadParamValue :: TailParamValues] {
    override def displays(params: HeadParam :: TailParams, state: Displayer.State): TagMod =
      TagMod(displayer(params.head, state)).apply(paramGetter.displays(params.tail, state))

    override def values(params: HeadParam :: TailParams, valueMap: Map[String, Any]) =
      params.head.getValue(valueMap) :: paramGetter.values(params.tail, valueMap)
  }
}
