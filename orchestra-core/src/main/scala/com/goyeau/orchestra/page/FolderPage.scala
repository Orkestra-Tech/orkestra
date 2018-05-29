package com.goyeau.orchestra.page

import com.goyeau.orchestra.board.Board
import com.goyeau.orchestra.css.Global
import com.goyeau.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

object FolderPage {
  case class Props(name: String, parentBreadcrumb: Seq[String], childBoards: Seq[Board], ctrl: RouterCtl[PageRoute])

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .render_P { props =>
        <.div(
          <.h1(props.name),
          <.div(
            props.childBoards.zipWithIndex.toTagMod {
              case (board, index) =>
                <.div(
                  Global.Style.listItem(index % 2 == 0),
                  Global.Style.cell,
                  ^.cursor.pointer,
                  props.ctrl.setOnClick(BoardPageRoute(props.parentBreadcrumb :+ props.name, board))
                )(board.name)
            }
          )
        )
      }
      .build
}
