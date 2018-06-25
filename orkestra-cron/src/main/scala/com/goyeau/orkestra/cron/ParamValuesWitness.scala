package com.goyeau.orkestra.cron

import shapeless._

trait ParamValuesWitness[DefaultParamValues <: HList, ParamValues <: HList] {
  def apply(params: DefaultParamValues): ParamValues
}

object ParamValuesWitness {
  implicit val hNil = new ParamValuesWitness[HNil, HNil] {
    override def apply(params: HNil) = HNil
  }

  implicit def hCons[HeadParamValue, TailParamValuesNoBranch <: HList, TailParamValues <: HList](
                                                                                                  implicit tailBranchInjector: ParamValuesWitness[TailParamValuesNoBranch, TailParamValues],
                                                                                                  ev: HeadParamValue
  ) = new ParamValuesWitness[HeadParamValue :: TailParamValuesNoBranch, HeadParamValue :: TailParamValues] {

    override def apply(params: HeadParamValue :: TailParamValuesNoBranch) =
      params.head :: tailBranchInjector(params.tail)
  }
}
