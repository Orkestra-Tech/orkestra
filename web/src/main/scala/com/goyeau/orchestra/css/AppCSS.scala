package com.goyeau.orchestra.css

import com.goyeau.orchestra.components.TopNav
import com.goyeau.orchestra.pages.StatusPage
import scalacss.ScalaCssReact._
import scalacss.Defaults._
import scalacss.internal.mutable.GlobalRegistry

object AppCSS {

  def load = {
    GlobalRegistry.register(GlobalStyle, TopNav.Style, StatusPage.Style)
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
