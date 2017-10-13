package io.chumps.orchestra.board

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._

import io.chumps.orchestra.page.FolderBoardPage
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {

  def route = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    staticRoute(pathName, BoardPageRoute(this)) ~>
      renderR(ctrl => FolderBoardPage.component(FolderBoardPage.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(pathName)
  }
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}
