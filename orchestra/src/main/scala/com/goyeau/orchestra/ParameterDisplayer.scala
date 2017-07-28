package com.goyeau.orchestra

import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.syntax.typeable._

trait ParameterDisplayer[P <: Parameter[_]] {
  def apply(param: P, state: ParameterDisplayer.State): TagMod
}

object ParameterDisplayer extends LowPriorityDisplayers {
  case class State(updated: ((String, Any)) => Callback, get: String => Option[Any]) {
    def +(kv: (String, Any)) = updated(kv)
  }

  implicit val stringDisplayer = new ParameterDisplayer[Param[String]] {
    def apply(param: Param[String], state: State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.name -> event.target.value)
      }

      <.label(
        ^.display.block,
        <.span(param.name),
        <.input.text(
          ^.key := param.id,
          ^.value :=? state.get(param.name).flatMap(_.cast[String]).orElse(param.defaultValue),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val intDisplayer = new ParameterDisplayer[Param[Int]] {
    def apply(param: Param[Int], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.name -> event.target.value.toInt)
      }

      <.label(
        ^.display.block,
        <.span(param.name),
        <.input.text(
          ^.key := param.id,
          ^.value :=? state.get(param.name).flatMap(_.cast[Int]).orElse(param.defaultValue).map(_.toString),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val booleanDisplayer = new ParameterDisplayer[Param[Boolean]] {
    def apply(param: Param[Boolean], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.name -> event.target.value.toBoolean)
      }

      <.label(
        ^.display.block,
        <.input.checkbox(
          ^.key := param.id,
          ^.value :=? state.get(param.name).flatMap(_.cast[Boolean]).orElse(param.defaultValue).map(_.toString),
          ^.onChange ==> modValue
        ),
        <.span(param.name)
      )
    }
  }
}

trait LowPriorityDisplayers {

  // Dont display by default
  implicit def default[P <: Parameter[_]] = new ParameterDisplayer[P] {
    def apply(param: P, state: ParameterDisplayer.State) = TagMod()
  }
}
