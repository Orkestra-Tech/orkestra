package com.goyeau.orchestra

import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import japgolly.scalajs.react.vdom.TagMod
import shapeless.{::, HList, HNil, Poly}
import shapeless.ops.hlist.{LeftFolder, Prepend, ToTraversable, Tupler}

trait ParamGetter[Params <: HList, ParamValues] {
  def displays(params: Params, $ : RenderScope[Unit, Map[String, Any], Unit]): TagMod
  def values(params: Params, map: Map[String, Any]): ParamValues
}

object ParamGetter {
  implicit val noParameter = new ParamGetter[HNil, Unit] {
    override def values(params: HNil, map: Map[String, Any]) = ()

    override def displays(params: HNil, $ : RenderScope[Unit, Map[String, Any], Unit]): TagMod = TagMod()
  }

  implicit def oneParameter[ParamValue](implicit displayer: Displayer[Parameter[ParamValue]]) =
    new ParamGetter[Parameter[ParamValue] :: HNil, ParamValue] {
      override def values(params: Parameter[ParamValue] :: HNil, map: Map[String, Any]): ParamValue =
        params.head.getValue(map)

      override def displays(params: Parameter[ParamValue] :: HNil,
                            $ : RenderScope[Unit, Map[String, Any], Unit]): TagMod = displayer(params.head, $)
    }

  implicit def multiParameters[Params <: HList, Disps <: HList, ParamValues <: HList, ParamValue](
    implicit valuesGetter: LeftFolder.Aux[Params,
                                          (HNil.type, Map[String, Any]),
                                          GetValues.type,
                                          (ParamValues, Map[String, Any])],
    tupler: Tupler.Aux[ParamValues, ParamValue],
    displaysGetter: LeftFolder.Aux[Params,
                                   (HNil.type, RenderScope[Unit, Map[String, Any], Unit]),
                                   GetDisplays.type,
                                   (Disps, RenderScope[Unit, Map[String, Any], Unit])],
    toSeq: ToTraversable.Aux[Disps, Seq, TagMod]
  ) = new ParamGetter[Params, ParamValue] {
    override def values(params: Params, map: Map[String, Any]) = tupler(params.foldLeft((HNil, map))(GetValues)._1)

    override def displays(params: Params, $ : RenderScope[Unit, Map[String, Any], Unit]): TagMod =
      TagMod(toSeq(params.foldLeft((HNil, $))(GetDisplays)._1): _*)
  }

  object GetDisplays extends Poly {
    implicit def forParameter[Disps <: HList, P <: Parameter[_]](
      implicit displayer: Displayer[P],
      prepend: Prepend[Disps, TagMod :: HNil]
    ) =
      use((acc: (Disps, RenderScope[Unit, Map[String, Any], Unit]), p: P) => (acc._1 :+ displayer(p, acc._2), acc._2))
  }

  object GetValues extends Poly {
    implicit def forParameter[ParamValues <: HList, P, T](
      implicit paramT: P <:< Parameter[T],
      prepend: Prepend[ParamValues, T :: HNil]
    ) =
      use((acc: (ParamValues, Map[String, Any]), p: P) => (acc._1 :+ paramT(p).getValue(acc._2), acc._2))
  }
}
