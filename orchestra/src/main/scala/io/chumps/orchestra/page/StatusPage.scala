package io.chumps.orchestra.page

import scalacss.ScalaCssReact._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object StatusPage {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._
    val content = style(textAlign.center, fontSize(30.px), minHeight(450.px), paddingTop(40.px))
  }

  val component =
    ScalaComponent.builder.static(getClass.getSimpleName)(<.main(Style.content, "Status")).build

  def apply() = component()
}
