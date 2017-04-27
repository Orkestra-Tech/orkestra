package com.goyeau.orchestra

import scala.scalajs.js.JSApp

import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.AppRouter
import org.scalajs.dom

object ReactApp extends JSApp {

  override def main(): Unit = {
    AppCSS.load
    AppRouter.router().renderIntoDOM(dom.document.body)
  }
}
