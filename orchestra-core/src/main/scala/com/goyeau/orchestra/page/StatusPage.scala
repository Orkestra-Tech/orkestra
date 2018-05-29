package com.goyeau.orchestra.page

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object StatusPage {
  val component = ScalaComponent.builder.static(getClass.getSimpleName)(<.div(<.h1("Status"))).build
}
