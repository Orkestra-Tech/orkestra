package io.chumps.orchestra.route

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._

import io.chumps.orchestra.component.{Footer, TopNav}
import io.chumps.orchestra._
import io.chumps.orchestra.board.Board
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.page.{LogsPage, StatusPage}

object WebRouter {

  sealed trait PageRoute
  case class BoardPageRoute(breadcrumb: Seq[String], runId: Option[RunId] = None) extends PageRoute
  case class LogsPageRoute(runId: RunId) extends PageRoute
  case object StatusPageRoute extends PageRoute

  private def config(board: Board) = RouterConfigDsl[PageRoute].buildConfig { dsl =>
    import dsl._

    val rootBoard = BoardPageRoute(Seq(board.pathName))
    val logsRoute =
      dynamicRouteCT(uuid.xmap(uuid => LogsPageRoute(RunId(uuid)))(_.runId.value)) ~> dynRender(
        page => LogsPage.component(LogsPage.Props(page))
      )
    val statusRoute = staticRoute(root, StatusPageRoute) ~> render(StatusPage())

    (trimSlashes |
      (
        board.route(Seq.empty).prefixPath_/("boards") |
          logsRoute.prefixPath_/("logs") |
          statusRoute.prefixPath_/("status")
      ).prefixPath_/("#"))
      .notFound(redirectToPage(rootBoard)(Redirect.Replace))
      .renderWith { (ctl: RouterCtl[PageRoute], resolution: Resolution[PageRoute]) =>
        <.div(
          TopNav.component(TopNav.Props(rootBoard, resolution.page, ctl)),
          resolution.render(),
          Footer.component()
        )
      }
  }

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()
}
