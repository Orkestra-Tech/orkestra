package io.chumps.orchestra.page

import io.chumps.orchestra.Board
import io.chumps.orchestra.route.WebRouter.{AppPage, BoardPage}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._

object FolderBoardPage {
  case class Props(name: String, childBoards: Seq[Board], ctrl: RouterCtl[AppPage])

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .render_P { props =>
        <.div(
          <.div(props.name),
          <.div(
            props.childBoards.toTagMod(board => <.div(<.button(props.ctrl.setOnClick(BoardPage(board)), board.name)))
          )
        )
      }
      .build
}
