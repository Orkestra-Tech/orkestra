package com.goyeau.orchestra.routes

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.goyeau.orchestra.components.{Footer, TopNav}
import com.goyeau.orchestra.models.Menu
import com.goyeau.orchestra.pages.StatusPage
import com.goyeau.orchestra._

object AppRouter {

  sealed trait AppPage

  case class Boards(p: Board) extends AppPage
  case object Status extends AppPage

  def config(board: Board) = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    val mainMenu = Vector(
      Menu("Boards", Boards(board)),
      Menu("Status", Status)
    )

    def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) =
      <.div(
        TopNav(TopNav.Props(mainMenu, r.page, c)),
        r.render(),
        Footer()
      )

    val boardRoute = board.route.pmap[AppPage](Boards) {
      case Boards(p) => p
    }

    (trimSlashes | (boardRoute | staticRoute("status", Status) ~> render(StatusPage())).prefixPath_/("#"))
      .notFound(redirectToPage(Boards(board))(Redirect.Replace))
      .renderWith(layout)
  }

  def router(board: Board) = Router(BaseUrl.fromWindowOrigin, config(board))()

}
