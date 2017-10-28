package io.chumps.orchestra.css

import io.chumps.orchestra.component.{StopButton, TopNav}
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.internal.mutable.GlobalRegistry

import io.chumps.orchestra.page.StatusPage

object AppCss {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  def load() = {
    GlobalRegistry.register(Global.Style, TopNav.Style, StopButton.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
