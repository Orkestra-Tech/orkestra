package com.goyeau.orchestra

import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.AppRouter

object ReactApp extends JSApp {

  @JSExport
  override def main(): Unit = {
    AppCSS.load
    AppRouter.router().renderIntoDOM(dom.document.body)
  }

}
