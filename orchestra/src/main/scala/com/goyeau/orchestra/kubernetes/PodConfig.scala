package com.goyeau.orchestra.kubernetes

import shapeless._
import shapeless.ops.hlist.ToTraversable

case class PodConfig[Containers <: HList](containers: Containers = HNil, nodeSelector: Map[String, String] = Map.empty)(
  implicit toSeq: ToTraversable.Aux[Containers, Seq, Container]
) {
  private[orchestra] val containerSeq: Seq[Container] = toSeq(containers)
}

//object PodConfig {
//
//  def apply[TupledContainers <: Product, Containers <: HList](containers: TupledContainers)(
//    implicit tupleToHList: Generic.Aux[TupledContainers, Containers],
//    toSeq: ToTraversable.Aux[Containers, Seq, Container]
//  ): PodConfig[Containers] =
//    PodConfig(tupleToHList.to(containers), Seq.empty)
//
//  def apply[C <: Container](container: C): PodConfig[C :: HNil] =
//    PodConfig(container :: HNil, Seq.empty)
//}

case class Container(name: String, image: String, tty: Boolean = false, command: Seq[String] = Seq.empty)
