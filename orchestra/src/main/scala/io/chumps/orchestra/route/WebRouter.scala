package io.chumps.orchestra.route

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

import io.chumps.orchestra.component.{Footer, TopNav}
import io.chumps.orchestra._
import io.chumps.orchestra.board.{Board, Folder, Job}
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.page.{LogsPage, StatusPage}

object WebRouter {

  sealed trait PageRoute
  case class BoardPageRoute(breadcrumb: Seq[String], runId: Option[RunId] = None) extends PageRoute
  case class LogsPageRoute(runId: RunId) extends PageRoute
  case object StatusPageRoute extends PageRoute

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

  private def config(board: Board) = RouterConfigDsl[PageRoute].buildConfig { dsl =>
    import dsl._

    val rootPage = BoardPageRoute(Seq(board.id.name.toLowerCase))
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
      .notFound(redirectToPage(rootPage)(Redirect.Replace))
      .renderWith { (ctl: RouterCtl[PageRoute], resolution: Resolution[PageRoute]) =>
        <.div(
          TopNav.component(TopNav.Props(rootPage, resolution.page, ctl, jobs(board))),
          resolution.render(),
          Footer.component()
        )
      }
  }

  private def jobs(board: Board): Seq[Job[_, _ <: HList, _]] = board match {
    case folder: Folder    => folder.childBoards.flatMap(jobs)
    case job: Job[_, _, _] => Seq(job)
  }
}
