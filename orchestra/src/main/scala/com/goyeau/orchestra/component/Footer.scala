package com.goyeau.orchestra.component

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Footer {

  val component = ScalaComponent.builder
    .static("Footer")(
      <.footer(
        ^.textAlign.center,
        <.div(^.borderBottom := "1px solid grey", ^.padding := "0px"),
        <.p(^.paddingTop := "5px", "Footer")
      )
    )
    .build

  def apply() = component()
}
