package com.goyeau.orkestra.component

import com.goyeau.orkestra.BuildInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object Footer {

  val component = ScalaComponent.builder
    .static(getClass.getSimpleName)(
      <.p(s"${BuildInfo.projectName} ${BuildInfo.version}")
    )
    .build
}
