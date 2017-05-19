package com.goyeau.orchestra

import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil, Poly}
import shapeless.ops.hlist.{LeftFolder, Prepend, ToTraversable}

trait ParamGetter[Params <: HList, ParamValues <: HList] {
  def displays(params: Params, state: Displayer.State): TagMod
  def values(params: Params, map: Map[String, Any]): ParamValues
}

object ParamGetter {

  implicit val hnil = new ParamGetter[HNil, HNil] {
    override def displays(params: HNil, state: Displayer.State): TagMod = TagMod()

    override def values(params: HNil, map: Map[String, Any]) = HNil
  }

  implicit def apply[Params <: HList, Disps <: HList, ParamValues <: HList](
    implicit displaysGetter: LeftFolder.Aux[Params,
                                            (HNil.type, Displayer.State),
                                            GetDisplays.type,
                                            (Disps, Displayer.State)],
    valuesGetter: LeftFolder.Aux[Params,
                                 (HNil.type, Map[String, Any]),
                                 GetValues.type,
                                 (ParamValues, Map[String, Any])],
    toSeq: ToTraversable.Aux[Disps, Seq, TagMod]
  ) = new ParamGetter[Params, ParamValues] {
    override def displays(params: Params, state: Displayer.State): TagMod =
      TagMod(toSeq(params.foldLeft((HNil, state))(GetDisplays)._1): _*)

    override def values(params: Params, map: Map[String, Any]) = params.foldLeft((HNil, map))(GetValues)._1
  }

  object GetDisplays extends Poly {
    implicit def forParameter[Disps <: HList, Param <: Parameter[_]](
      implicit displayer: Displayer[Param],
      prepend: Prepend[Disps, TagMod :: HNil]
    ) =
      use((acc: (Disps, Displayer.State), p: Param) => (acc._1 :+ displayer(p, acc._2), acc._2))
  }

  object GetValues extends Poly {
    implicit def forParameter[ParamValues <: HList, Param, ParamValue]( // TODO: Ask Aldo why
      implicit paramEv: Param <:< Parameter[ParamValue],
      prepend: Prepend[ParamValues, ParamValue :: HNil]
    ) =
      use((acc: (ParamValues, Map[String, Any]), p: Param) => (acc._1 :+ paramEv(p).getValue(acc._2), acc._2))
  }
}
