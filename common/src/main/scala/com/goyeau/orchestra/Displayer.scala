package com.goyeau.orchestra

import japgolly.scalajs.react.ReactEventFromInput
import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.syntax.typeable._

trait Displayer[T] { // TODO restrict to Parameter[_] ?
  def apply(p: T, $ : RenderScope[_, Map[String, Any], _]): TagMod
}

object Displayer extends LowPriorityDisplayers {
  implicit val stringDisplayer = new Displayer[Param[String]] {
    def apply(p: Param[String], $ : RenderScope[_, Map[String, Any], _]) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        $.modState(_ + (p.name -> event.target.value))
      }

      TagMod(
        <.input.text(
          ^.key := p.name,
          ^.value :=? $.state.get(p.name).flatMap(_.cast[String]).orElse(p.defaultValue),
          ^.onChange ==> modValue
        )
      )
    }
  }

  implicit val intDisplayer = new Displayer[Param[Int]] {
    def apply(p: Param[Int], $ : RenderScope[_, Map[String, Any], _]) = {
      def modValue(event: ReactEventFromInput) = {
        event.persist()
        $.modState(_ + (p.name -> event.target.value.toInt))
      }

      TagMod(
        <.input.text(
          ^.key := p.name,
          ^.value :=? $.state.get(p.name).flatMap(_.cast[Int]).orElse(p.defaultValue).map(_.toString),
          ^.onChange ==> modValue
        )
      )
    }
  }
}

trait LowPriorityDisplayers {

  // Dont display by default
  implicit def default[T] = new Displayer[T] {
    def apply(p: T, $ : RenderScope[_, Map[String, Any], _]) = TagMod()
  }
}
