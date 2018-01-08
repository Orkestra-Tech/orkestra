package io.chumps.orchestra.component

import io.chumps.orchestra.BuildInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Footer {

  val component = ScalaComponent.builder
    .static(getClass.getSimpleName)(
      <.footer(
        ^.textAlign.center,
        <.div(^.borderBottom := "1px solid grey", ^.padding := "0px"),
        <.p(^.padding := "5px", s"${BuildInfo.projectName} ${BuildInfo.version}")
      )
    )
    .build
}
