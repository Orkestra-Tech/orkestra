package com.goyeau.orchestra.github

import shapeless._

trait GitRefInjector[ParamValuesNoBranch <: HList, ParamValues <: HList] {
  def apply(params: ParamValuesNoBranch, ref: GitRef): ParamValues
}

object GitRefInjector {
  implicit val hNil = new GitRefInjector[HNil, HNil] {
    override def apply(params: HNil, ref: GitRef) = HNil
  }

  implicit def hConsBranch[ParamValuesNoBranch <: HList, TailParamValues <: HList](
    implicit tailRunIdInjector: GitRefInjector[ParamValuesNoBranch, TailParamValues]
  ) = new GitRefInjector[ParamValuesNoBranch, GitRef :: TailParamValues] {

    override def apply(valuesNoRunId: ParamValuesNoBranch, ref: GitRef) =
      ref :: tailRunIdInjector(valuesNoRunId, ref)
  }

  implicit def hCons[HeadParamValue, TailParamValuesNoBranch <: HList, TailParamValues <: HList](
    implicit tailBranchInjector: GitRefInjector[TailParamValuesNoBranch, TailParamValues],
    ev: HeadParamValue <:!< GitRef
  ) = new GitRefInjector[HeadParamValue :: TailParamValuesNoBranch, HeadParamValue :: TailParamValues] {

    override def apply(params: HeadParamValue :: TailParamValuesNoBranch, ref: GitRef) =
      params.head :: tailBranchInjector(params.tail, ref)
  }
}
