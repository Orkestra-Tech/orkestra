package com.goyeau.orchestra

import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil, Poly}
import shapeless.ops.hlist.{LeftFolder, Prepend, ToTraversable, Tupler}

trait ParamGetter[Params <: HList, ParamValues] {
  def displays(params: Params, state: Displayer.State): TagMod
  def values(params: Params, map: Map[String, Any]): ParamValues
}

object ParamGetter {
  implicit val noParameter = new ParamGetter[HNil, Unit] {
    override def displays(params: HNil, state: Displayer.State): TagMod = TagMod()

    override def values(params: HNil, map: Map[String, Any]) = ()
  }

  implicit def oneParameter[Param <: Parameter[ParamValue], ParamValue](implicit displayer: Displayer[Param]) =
    new ParamGetter[Param :: HNil, ParamValue] {
      override def displays(params: Param :: HNil, state: Displayer.State): TagMod =
        displayer(params.head, state)

      override def values(params: Param :: HNil, map: Map[String, Any]): ParamValue =
        params.head.getValue(map)
    }

  implicit def multiParameters[Params <: HList, Disps <: HList, ParamValuesL <: HList, ParamValues](
    implicit displaysGetter: LeftFolder.Aux[Params,
                                            (HNil.type, Displayer.State),
                                            GetDisplays.type,
                                            (Disps, Displayer.State)],
    valuesGetter: LeftFolder.Aux[Params,
                                 (HNil.type, Map[String, Any]),
                                 GetValues.type,
                                 (ParamValuesL, Map[String, Any])],
    tupler: Tupler.Aux[ParamValuesL, ParamValues],
    toSeq: ToTraversable.Aux[Disps, Seq, TagMod]
  ) = new ParamGetter[Params, ParamValues] {
    override def displays(params: Params, state: Displayer.State): TagMod =
      TagMod(toSeq(params.foldLeft((HNil, state))(GetDisplays)._1): _*)

    override def values(params: Params, map: Map[String, Any]) = tupler(params.foldLeft((HNil, map))(GetValues)._1)
  }

  object GetDisplays extends Poly {
    implicit def forParameter[Disps <: HList, P <: Parameter[_]](
      implicit displayer: Displayer[P],
      prepend: Prepend[Disps, TagMod :: HNil]
    ) =
      use((acc: (Disps, Displayer.State), p: P) => (acc._1 :+ displayer(p, acc._2), acc._2))
  }

  object GetValues extends Poly {
    implicit def forParameter[ParamValues <: HList, P, T](
      implicit paramT: P <:< Parameter[T],
      prepend: Prepend[ParamValues, T :: HNil]
    ) =
      use((acc: (ParamValues, Map[String, Any]), p: P) => (acc._1 :+ paramT(p).getValue(acc._2), acc._2))
  }
}
