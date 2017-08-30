package com.goyeau.orchestra.component

import com.goyeau.orchestra.BuildInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Footer {

  val component = ScalaComponent.builder
    .static("Footer")(
      <.footer(
        ^.textAlign.center,
        <.div(^.borderBottom := "1px solid grey", ^.padding := "0px"),
        <.p(^.padding := "5px", s"${BuildInfo.name} ${BuildInfo.version}")
      )
    )
    .build

  def apply() = component()
}
