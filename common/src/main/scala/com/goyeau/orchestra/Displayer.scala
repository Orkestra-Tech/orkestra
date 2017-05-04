package com.goyeau.orchestra

import japgolly.scalajs.react.{Callback, ReactEventFromInput}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.syntax.typeable._

trait Displayer[T <: Parameter[_]] {
  def apply(p: T, state: Displayer.State): TagMod
}

object Displayer extends LowPriorityDisplayers {
  case class State(updated: ((String, Any)) => Callback, get: String => Option[Any]) {
    def +(kv: (String, Any)) = updated(kv)
  }

  implicit val stringDisplayer = new Displayer[Param[String]] {
    def apply(p: Param[String], state: State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (p.name -> event.target.value)
      }

      TagMod(
        <.input.text(
          ^.key := p.name,
          ^.value :=? state.get(p.name).flatMap(_.cast[String]).orElse(p.defaultValue),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val intDisplayer = new Displayer[Param[Int]] {
    def apply(p: Param[Int], state: Displayer.State) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        state + (p.name -> event.target.value.toInt)
      }

      TagMod(
        <.input.text(
          ^.key := p.name,
          ^.value :=? state.get(p.name).flatMap(_.cast[Int]).orElse(p.defaultValue).map(_.toString),
          ^.onChange ==> modValue
        )
      )
    }
  }
}

trait LowPriorityDisplayers {

  // Dont display by default
  implicit def default[T <: Parameter[_]] = new Displayer[T] {
    def apply(p: T, state: Displayer.State) = TagMod()
  }
}
