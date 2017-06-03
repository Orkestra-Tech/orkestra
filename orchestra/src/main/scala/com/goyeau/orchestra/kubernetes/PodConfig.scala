package com.goyeau.orchestra.kubernetes

import shapeless.ops.hlist.ToTraversable
import shapeless.{Generic, HList}

case class PodConfig[Containers <: HList](containers: Containers)(
  implicit toSeq: ToTraversable.Aux[Containers, Seq, Container]
) {
  private[orchestra] val containerSeq: Seq[Container] = toSeq(containers)
}

object PodConfig {
  def apply[TupledContainers, Containers <: HList](containers: TupledContainers)(
    implicit tupleToHList: Generic.Aux[TupledContainers, Containers],
    toSeq: ToTraversable.Aux[Containers, Seq, Container]
  ): PodConfig[Containers] = {
    val containersHList = tupleToHList.to(containers)
    PodConfig(containersHList)
  }
}

case class Container(name: String, image: String)
