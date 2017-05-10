package com.goyeau.orchestra

import scala.scalajs.js.JSApp

import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.{Backend, WebRouter}
import org.scalajs.dom

trait Orchestra extends JSApp {
  def board: Board

  // Web main
  override def main(): Unit = {
    AppCSS.load
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById("orchestra"))
  }

  // Backend main
  def main(args: Array[String]): Unit =
    Backend(board).startServer("localhost", 1234)
}
