package com.goyeau.orchestra.route

import java.util.UUID

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.goyeau.orchestra.component.{Footer, TopNav}
import com.goyeau.orchestra.model.Menu
import com.goyeau.orchestra._

import com.goyeau.orchestra.page.StatusPage
import shapeless.HList

object WebRouter {

  sealed trait AppPage
  case class BoardPage2(path: String) extends AppPage
  case class BoardPage(board: Board) extends AppPage
  case class TaskLogsPage(job: Job.Definition[_, _ <: HList, _], runId: UUID) extends AppPage
  case object Status extends AppPage

  def config(board: Board) = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    val rootBoard = BoardPage(board)

    val mainMenu = Vector(
      Menu("Boards", rootBoard),
      Menu("Status", Status)
    )

    def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) =
      <.div(
        TopNav(TopNav.Props(mainMenu, r.page, c)),
        r.render(),
        Footer()
      )

    (trimSlashes | (board.route | staticRoute("status", Status) ~> render(StatusPage())).prefixPath_/("#"))
      .notFound(redirectToPage(rootBoard)(Redirect.Replace))
      .renderWith(layout)
  }

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

}
