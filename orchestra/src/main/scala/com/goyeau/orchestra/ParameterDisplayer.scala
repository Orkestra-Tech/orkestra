package com.goyeau.orchestra

import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.syntax.typeable._

trait ParameterDisplayer[P <: Parameter[_]] {
  def apply(p: P, state: ParameterDisplayer.State): TagMod
}

object ParameterDisplayer extends LowPriorityDisplayers {
  case class State(updated: ((String, Any)) => Callback, get: String => Option[Any]) {
    def +(kv: (String, Any)) = updated(kv)
  }

  implicit val stringDisplayer = new ParameterDisplayer[Param[String]] {
    def apply(p: Param[String], state: State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (p.name -> event.target.value)
      }

      <.input.text(
        ^.key := p.name,
        ^.value :=? state.get(p.name).flatMap(_.cast[String]).orElse(p.defaultValue),
        ^.onChange ==> modValue
      )
    }
  }

  implicit val intDisplayer = new ParameterDisplayer[Param[Int]] {
    def apply(p: Param[Int], state: ParameterDisplayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (p.name -> event.target.value.toInt)
      }

      <.input.text(
        ^.key := p.name,
        ^.value :=? state.get(p.name).flatMap(_.cast[Int]).orElse(p.defaultValue).map(_.toString),
        ^.onChange ==> modValue
      )
    }
  }
}

trait LowPriorityDisplayers {

  // Dont display by default
  implicit def default[P <: Parameter[_]] = new ParameterDisplayer[P] {
    def apply(p: P, state: ParameterDisplayer.State) = TagMod()
  }
}
