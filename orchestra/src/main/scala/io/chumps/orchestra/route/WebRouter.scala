package io.chumps.orchestra.route

import java.util.UUID

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._

import io.chumps.orchestra.component.{Footer, TopNav}
import io.chumps.orchestra._
import io.chumps.orchestra.page.{LogsPage, StatusPage}

object WebRouter {

  sealed trait PageRoute
  case class BoardPageRoute(board: Board) extends PageRoute
  case class LogsPageRoute(runId: UUID) extends PageRoute
  case object StatusPageRoute extends PageRoute

  def config(board: Board) = RouterConfigDsl[PageRoute].buildConfig { dsl =>
    import dsl._

    val rootBoard = BoardPageRoute(board)
    val logsRoute =
      dynamicRouteCT(uuid.caseClass[LogsPageRoute]) ~> dynRender(page => LogsPage.component(LogsPage.Props(page)))
    val statusRoute = staticRoute(root, StatusPageRoute) ~> render(StatusPage())

    (trimSlashes |
      (
        board.route.prefixPath_/("boards") | logsRoute.prefixPath_/("logs") | statusRoute.prefixPath_/("status")
      ).prefixPath_/("#"))
      .notFound(redirectToPage(rootBoard)(Redirect.Replace))
      .renderWith { (ctl: RouterCtl[PageRoute], resolution: Resolution[PageRoute]) =>
        <.div(
          TopNav.component(TopNav.Props(rootBoard, resolution.page, ctl)),
          resolution.render(),
          Footer()
        )
      }
  }

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

}
