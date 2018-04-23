package com.drivetribe.orchestra.component

import com.drivetribe.orchestra.BuildInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Footer {

  val component = ScalaComponent.builder
    .static(getClass.getSimpleName)(
      <.p(s"${BuildInfo.projectName} ${BuildInfo.version}")
    )
    .build
}
