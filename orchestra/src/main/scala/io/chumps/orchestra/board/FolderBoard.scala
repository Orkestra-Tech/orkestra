package io.chumps.orchestra.board

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._

import io.chumps.orchestra.page.FolderBoardPage
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    val breadcrumb = parentBreadcrumb :+ pathName
    staticRoute(pathName, BoardPageRoute(breadcrumb)) ~>
      renderR(ctrl => FolderBoardPage.component(FolderBoardPage.Props(name, breadcrumb, childBoards, ctrl))) |
      childBoards.map(_.route(breadcrumb)).reduce(_ | _).prefixPath_/(pathName)
  }
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}
