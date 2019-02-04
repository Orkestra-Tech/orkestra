package tech.orkestra.github

import shapeless._

trait GitRefInjector[ParametersNoGifRef <: HList, Parameters <: HList] {
  def apply(params: ParametersNoGifRef, ref: GitRef): Parameters
}

object GitRefInjector {
  implicit val hNil = new GitRefInjector[HNil, HNil] {
    override def apply(params: HNil, ref: GitRef) = HNil
  }

  implicit def hConsGitRef[ParametersNoBranch <: HList, TailParameters <: HList](
    implicit tailRunIdInjector: GitRefInjector[ParametersNoBranch, TailParameters]
  ) = new GitRefInjector[ParametersNoBranch, GitRef :: TailParameters] {

    override def apply(valuesNoRunId: ParametersNoBranch, ref: GitRef) =
      ref :: tailRunIdInjector(valuesNoRunId, ref)
  }

  implicit def hCons[HeadParamValue, TailParametersNoBranch <: HList, TailParameters <: HList](
    implicit tailBranchInjector: GitRefInjector[TailParametersNoBranch, TailParameters]
  ) = new GitRefInjector[HeadParamValue :: TailParametersNoBranch, HeadParamValue :: TailParameters] {

    override def apply(params: HeadParamValue :: TailParametersNoBranch, ref: GitRef) =
      params.head :: tailBranchInjector(params.tail, ref)
  }
}
