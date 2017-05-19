package com.goyeau.orchestra

import scala.scalajs.js.JSApp

import com.goyeau.orchestra.Task.Runner
import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.{Backend, WebRouter}
import io.circe.shapes.HListInstances
import org.scalajs.dom
import shapeless._

trait Orchestra extends JSApp with HListInstances {

  def registedTasks: Seq[Runner[_, _, _]]
  def board: Board

  // Web main
  override def main(): Unit = {
    AppCSS.load
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById("orchestra"))
  }

  // Backend main
  def main(args: Array[String]): Unit =
    Backend(registedTasks).startServer("localhost", 1234)
}
