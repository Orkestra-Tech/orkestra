package io.chumps.orchestra.route

import java.util.UUID

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import io.chumps.orchestra.component.{Footer, TopNav}
import io.chumps.orchestra._
import io.chumps.orchestra.page.StatusPage
import shapeless.HList

object WebRouter {

  sealed trait AppPage
  case class BoardPage(board: Board) extends AppPage
  case class TaskLogsPage(job: Job.Definition[_, _ <: HList, _], runId: UUID) extends AppPage
  case object Status extends AppPage

  def config(board: Board) = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    val rootBoard = BoardPage(board)

    (trimSlashes | (board.route | staticRoute("status", Status) ~> render(StatusPage())).prefixPath_/("#"))
      .notFound(redirectToPage(rootBoard)(Redirect.Replace))
      .renderWith { (ctl: RouterCtl[AppPage], resolution: Resolution[AppPage]) =>
        <.div(
          TopNav(TopNav.Props(rootBoard, resolution.page, ctl)),
          resolution.render(),
          Footer()
        )
      }
  }

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

}
