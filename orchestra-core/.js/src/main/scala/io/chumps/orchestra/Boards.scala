package io.chumps.orchestra

import io.chumps.orchestra.css.AppCss
import io.chumps.orchestra.route.WebRouter
import org.scalajs.dom

import io.chumps.orchestra.board.Board

trait Boards {
  def board: Board

  def main(args: Array[String]): Unit = {
    AppCss.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(BuildInfo.projectName.toLowerCase))
  }
}
