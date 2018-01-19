package io.chumps.orchestra.github

import shapeless._

trait BranchInjector[ParamValuesNoBranch <: HList, ParamValues <: HList] {
  def apply(params: ParamValuesNoBranch, branch: Branch): ParamValues
}

object BranchInjector {
  implicit val hNil = new BranchInjector[HNil, HNil] {
    override def apply(params: HNil, runId: Branch) = HNil
  }

  implicit def hConsBranch[ParamValuesNoBranch <: HList, TailParamValues <: HList](
    implicit tailRunIdInjector: BranchInjector[ParamValuesNoBranch, TailParamValues]
  ) = new BranchInjector[ParamValuesNoBranch, Branch :: TailParamValues] {

    override def apply(valuesNoRunId: ParamValuesNoBranch, branch: Branch) =
      branch :: tailRunIdInjector(valuesNoRunId, branch)
  }

  implicit def hCons[HeadParamValue, TailParamValuesNoBranch <: HList, TailParamValues <: HList](
    implicit tailBranchInjector: BranchInjector[TailParamValuesNoBranch, TailParamValues],
    ev: HeadParamValue <:!< Branch
  ) = new BranchInjector[HeadParamValue :: TailParamValuesNoBranch, HeadParamValue :: TailParamValues] {

    override def apply(params: HeadParamValue :: TailParamValuesNoBranch, runId: Branch) =
      params.head :: tailBranchInjector(params.tail, runId)
  }
}
