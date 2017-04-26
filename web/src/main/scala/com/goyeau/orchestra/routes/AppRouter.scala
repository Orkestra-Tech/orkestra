package com.goyeau.orchestra.routes

import japgolly.scalajs.react.extra.router.{Resolution, RouterConfigDsl, RouterCtl, _}
import japgolly.scalajs.react.vdom.html_<^._
import com.goyeau.orchestra.components.{Footer, TopNav}
import com.goyeau.orchestra.models.Menu
import com.goyeau.orchestra.pages.StatusPage
import scalacss.ScalaCssReact._

import com.goyeau.orchestra._

object AppRouter {

  sealed trait AppPage

  case class Boards(p: Board) extends AppPage
  case object Status extends AppPage

  val emptyTask = Task {
    println("empty")
  }

  val deployBackend = Task(Param[String]("version"), RunId) { (version, runId) =>
    println(version + runId)
  }

  val rootBoard = FolderBoard("Drivetribe")(
    FolderBoard("Operation")(
      FolderBoard("Staging")(
        SingleTaskBoard("DeployBackend", deployBackend)
      )
    ),
    FolderBoard("Infrastructure")(
      FolderBoard("Staging")(
        SingleTaskBoard("Create", deployBackend)
      )
    )
  )

  val config = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._

    val boardRoute = rootBoard.route.pmap[AppPage](Boards) {
      case Boards(p) => p
    }

    (trimSlashes
      | (boardRoute | staticRoute("status", Status) ~> render(StatusPage())).prefixPath_/("#"))
      .notFound(redirectToPage(Boards(rootBoard))(Redirect.Replace))
      .renderWith(layout)
  }

  val mainMenu = Vector(
    Menu("Boards", Boards(rootBoard)),
    Menu("Status", Status)
  )

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) =
    <.div(
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      r.render(),
      Footer()
    )

  val router = Router(BaseUrl.fromWindowOrigin, config)

}
