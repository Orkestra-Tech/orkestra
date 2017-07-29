package com.goyeau.orchestra.css

import com.goyeau.orchestra.component.TopNav
import scalacss.ScalaCssReact._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.internal.mutable.GlobalRegistry

import com.goyeau.orchestra.page.StatusPage

object AppCSS {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  def load = {
    GlobalRegistry.register(Global.Style, TopNav.Style, StatusPage.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
