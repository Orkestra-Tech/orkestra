package com.drivetribe.orchestra.board

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._

import com.drivetribe.orchestra.page.FolderPage
import com.drivetribe.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class Folder(name: String, childBoards: Seq[Board]) extends Board {

  val segment = name.toLowerCase.replaceAll("\\s", "")

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    staticRoute(segment, BoardPageRoute(parentBreadcrumb, this)) ~>
      renderR(ctrl => FolderPage.component(FolderPage.Props(name, parentBreadcrumb, childBoards, ctrl))) |
      childBoards.map(_.route(parentBreadcrumb :+ name)).reduce(_ | _).prefixPath_/(segment)
  }
}

object Folder {
  def apply(name: String): (Board*) => Folder = (childBoards: Seq[Board]) => Folder(name, childBoards)
}
