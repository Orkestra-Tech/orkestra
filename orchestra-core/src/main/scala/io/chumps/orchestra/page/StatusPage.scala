package io.chumps.orchestra.page

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object StatusPage {

  val component =
    ScalaComponent.builder.static(getClass.getSimpleName)(<.main(<.h1("Status"))).build

  def apply() = component()
}
