package tech.orkestra.input

import japgolly.scalajs.react.vdom.TagMod
import shapeless._

trait InputOperations[Inputs <: HList, Parameters <: HList] {
  def displays(inputs: Inputs, state: State): Seq[TagMod]
  def values(inputs: Inputs, valueMap: Map[Symbol, Any]): Parameters
  def inputsState(inputs: Inputs, parameters: Parameters): Map[String, Any]
}

object InputOperations {

  implicit def hNil[Inputs <: HNil] = new InputOperations[Inputs, HNil] {
    override def displays(inputs: Inputs, state: State) = Seq.empty
    override def values(inputs: Inputs, valueMap: Map[Symbol, Any]) = HNil
    override def inputsState(inputs: Inputs, parameters: HNil) = Map.empty
  }

  implicit def hCons[HeadInput, TailInputs <: HList, HeadParameter, TailParameters <: HList](
    implicit tailInputOperations: InputOperations[TailInputs, TailParameters],
    headParam: HeadInput <:< Input[HeadParameter]
  ) = new InputOperations[HeadInput :: TailInputs, HeadParameter :: TailParameters] {

    override def displays(inputs: HeadInput :: TailInputs, state: State) =
      inputs.head.display(state) +: tailInputOperations.displays(inputs.tail, state)

    override def values(inputs: HeadInput :: TailInputs, valueMap: Map[Symbol, Any]) =
      inputs.head.getValue(valueMap) :: tailInputOperations.values(inputs.tail, valueMap)

    override def inputsState(inputs: HeadInput :: TailInputs, parameters: HeadParameter :: TailParameters) =
      tailInputOperations.inputsState(inputs.tail, parameters.tail) + (inputs.head.name -> parameters.head)
  }
}
