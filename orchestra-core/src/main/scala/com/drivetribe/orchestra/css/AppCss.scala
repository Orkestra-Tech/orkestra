package com.drivetribe.orchestra.css

import com.drivetribe.orchestra.component.{StopButton, TopNav}
import scalacss.internal.mutable.GlobalRegistry

object AppCss {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  def load() = {
    GlobalRegistry.register(Global.Style, TopNav.Style, StopButton.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
