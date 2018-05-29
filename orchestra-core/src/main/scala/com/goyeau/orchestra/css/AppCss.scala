package com.goyeau.orchestra.css

import com.goyeau.orchestra.component.{StopButton, TopNav}
import scalacss.internal.mutable.GlobalRegistry

object AppCss {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  def load() = {
    GlobalRegistry.register(Global.Style, TopNav.Style, StopButton.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
