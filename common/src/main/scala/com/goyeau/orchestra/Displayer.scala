package com.goyeau.orchestra

import scala.util.Random

import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.TopNode

trait Displayer[T] {
  def display(p: T): TagOf[_ <: TopNode]
}

object Displayer {
  implicit val stringDisplayer = new Displayer[Param[String]] {
    def display(p: Param[String]) = <.input(^.key := p.name, ^.value := p.name)
  }
  implicit val intDisplayer = new Displayer[Param[Int]] {
    def display(p: Param[Int]) = <.input(^.key := p.name, ^.value := p.name)
  }
}
