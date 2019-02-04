package tech.orkestra.cron

import shapeless._

trait ParametersWitness[DefaultParameters <: HList, Parameters <: HList] {
  def apply(params: DefaultParameters): Parameters
}

object ParametersWitness {
  implicit val hNil = new ParametersWitness[HNil, HNil] {
    override def apply(params: HNil) = HNil
  }

  implicit def hCons[HeadParamValue, TailParametersUnwitnessed <: HList, TailParameters <: HList](
    implicit tailParametersWitness: ParametersWitness[TailParametersUnwitnessed, TailParameters]
  ) = new ParametersWitness[HeadParamValue :: TailParametersUnwitnessed, HeadParamValue :: TailParameters] {

    override def apply(params: HeadParamValue :: TailParametersUnwitnessed) =
      params.head :: tailParametersWitness(params.tail)
  }
}
