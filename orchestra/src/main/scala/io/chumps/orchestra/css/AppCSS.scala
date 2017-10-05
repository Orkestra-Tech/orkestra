package io.chumps.orchestra.css

import io.chumps.orchestra.component.TopNav
import scalacss.ScalaCssReact._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.internal.mutable.GlobalRegistry

import io.chumps.orchestra.page.StatusPage

object AppCSS {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  def load() = {
    GlobalRegistry.register(Global.Style, TopNav.Style, StatusPage.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
