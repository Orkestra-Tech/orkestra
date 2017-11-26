package io.chumps.orchestra.route

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

import io.chumps.orchestra.component.{Footer, TopNav}
import io.chumps.orchestra._
import io.chumps.orchestra.board.{Board, Folder, Job}
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.page.StatusPage

object WebRouter {

  sealed trait PageRoute
  case class BoardPageRoute(breadcrumb: Seq[String], board: Board, runId: Option[RunId] = None) extends PageRoute
  case class LogsPageRoute(breadcrumb: Seq[String], runId: RunId) extends PageRoute
  case object StatusPageRoute extends PageRoute

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

  private def config(board: Board) = RouterConfigDsl[PageRoute].buildConfig { dsl =>
    import dsl._

    val rootPage = BoardPageRoute(Seq.empty, board)
    val statusRoute = staticRoute(root, StatusPageRoute) ~> render(StatusPage())
    val jobs = allJobs(board).foldLeft(emptyRule)((route, board) => route | board.route(Seq.empty))

    (trimSlashes |
      (
        board.route(Seq.empty).prefixPath_/("boards") |
          jobs.prefixPath_/("jobs") |
          LogsRoute(Seq.empty).prefixPath_/("logs") |
          statusRoute.prefixPath_/("status")
      ).prefixPath_/("#"))
      .notFound(redirectToPage(rootPage)(Redirect.Replace))
      .renderWith { (ctl: RouterCtl[PageRoute], resolution: Resolution[PageRoute]) =>
        <.div(
          TopNav.component(TopNav.Props(rootPage, resolution.page, ctl, allJobs(board))),
          resolution.render(),
          Footer.component()
        )
      }
  }

  private def allJobs(board: Board): Seq[Job[_ <: HList, _, _, _]] = board match {
    case folder: Folder       => folder.childBoards.flatMap(allJobs)
    case job: Job[_, _, _, _] => Seq(job)
  }
}
