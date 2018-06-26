package tech.orkestra.route

import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._

import tech.orkestra.model.RunId
import tech.orkestra.page.LogsPage
import tech.orkestra.route.WebRouter.{LogsPageRoute, PageRoute}

object LogsRoute {

  def apply(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    dynamicRoute(uuid.xmap(uuid => LogsPageRoute(parentBreadcrumb, RunId(uuid)))(_.runId.value)) {
      case p @ LogsPageRoute(`parentBreadcrumb`, _) => p
    } ~>
      dynRender(page => LogsPage.component(LogsPage.Props(page)))
  }
}
