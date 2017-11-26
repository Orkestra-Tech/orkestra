package io.chumps.orchestra.board

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._

import io.chumps.orchestra.page.FolderPage
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class Folder(name: String, childBoards: Seq[Board]) extends Board {

  val id = Symbol(name.toLowerCase.replaceAll("\\s", ""))

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    staticRoute(id.name, BoardPageRoute(parentBreadcrumb, this)) ~>
      renderR(ctrl => FolderPage.component(FolderPage.Props(name, parentBreadcrumb, childBoards, ctrl))) |
      childBoards.map(_.route(parentBreadcrumb :+ name)).reduce(_ | _).prefixPath_/(id.name)
  }
}

object Folder {
  def apply(name: String): (Board*) => Folder = (childBoards: Seq[Board]) => Folder(name, childBoards)
}
