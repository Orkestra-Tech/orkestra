package com.drivetribe.orchestra

import com.drivetribe.orchestra.css.AppCss
import com.drivetribe.orchestra.route.WebRouter
import org.scalajs.dom

import com.drivetribe.orchestra.board.Board

trait UI {
  def board: Board

  def main(args: Array[String]): Unit = {
    AppCss.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(BuildInfo.projectName.toLowerCase))
  }
}
