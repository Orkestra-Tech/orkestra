package com.goyeau.orchestra

import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.syntax.typeable._
import enumeratum.EnumEntry

trait ParameterDisplayer[P <: Parameter[_]] {
  def apply(param: P, state: ParameterDisplayer.State): TagMod
}

object ParameterDisplayer extends LowPriorityDisplayers {
  case class State(updated: ((Symbol, Any)) => Callback, get: Symbol => Option[Any]) {
    def +(kv: (Symbol, Any)) = updated(kv)
  }

  implicit val stringDisplayer = new ParameterDisplayer[Param[String]] {
    def apply(param: Param[String], state: State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.id -> event.target.value)
      }

      <.label(
        ^.display.block,
        <.span(param.name),
        <.input.text(
          ^.key := param.id.name,
          ^.value :=? state.get(param.id).flatMap(_.cast[String]).orElse(param.defaultValue),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val intDisplayer = new ParameterDisplayer[Param[Int]] {
    def apply(param: Param[Int], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.id -> event.target.value.toInt)
      }

      <.label(
        ^.display.block,
        <.span(param.name),
        <.input.text(
          ^.key := param.id.name,
          ^.value :=? state.get(param.id).flatMap(_.cast[Int]).orElse(param.defaultValue).map(_.toString),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val booleanDisplayer = new ParameterDisplayer[Param[Boolean]] {
    def apply(param: Param[Boolean], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.id -> event.target.checked)
      }

      <.label(
        ^.display.block,
        <.input.checkbox(
          ^.key := param.id.name,
          ^.checked := state.get(param.id).flatMap(_.cast[Boolean]).orElse(param.defaultValue).getOrElse(false),
          ^.onChange ==> modValue
        ),
        <.span(param.name)
      )
    }
  }

  implicit def enumDisplayer[Entry <: EnumEntry] = new ParameterDisplayer[EnumParam[Entry]] {
    def apply(param: EnumParam[Entry], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (param.id -> param.enum.withNameInsensitive(event.target.value))
      }

      <.label(
        ^.display.block,
        <.span(param.name),
        <.select(
          ^.key := param.id.name,
          ^.value :=? state.get(param.id).flatMap(_.cast[EnumEntry]).orElse(param.defaultValue).map(_.entryName),
          ^.onChange ==> modValue
        )(
          <.option(^.disabled := true, ^.selected := "selected")(param.name) +:
            param.enum.values.map(o => <.option(^.value := o.entryName)(o.toString)): _*
        )
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
