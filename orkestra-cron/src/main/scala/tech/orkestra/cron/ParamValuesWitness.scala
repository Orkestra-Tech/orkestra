package tech.orkestra.cron

import shapeless._

trait ParamValuesWitness[DefaultParamValues <: HList, ParamValues <: HList] {
  def apply(params: DefaultParamValues): ParamValues
}

object ParamValuesWitness {
  implicit val hNil = new ParamValuesWitness[HNil, HNil] {
    override def apply(params: HNil) = HNil
  }

  implicit def hCons[HeadParamValue, TailParamValuesUnwitnessed <: HList, TailParamValues <: HList](
    implicit tailParamValuesWitness: ParamValuesWitness[TailParamValuesUnwitnessed, TailParamValues]
  ) = new ParamValuesWitness[HeadParamValue :: TailParamValuesUnwitnessed, HeadParamValue :: TailParamValues] {

    override def apply(params: HeadParamValue :: TailParamValuesUnwitnessed) =
      params.head :: tailParamValuesWitness(params.tail)
  }
}
