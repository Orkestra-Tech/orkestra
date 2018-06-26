package tech.orkestra.board

import tech.orkestra.route.WebRouter.PageRoute
import japgolly.scalajs.react.extra.router.StaticDsl

trait Board {
  val segment: String
  val name: String
  def route(parentBreadcrumb: Seq[String]): StaticDsl.Rule[PageRoute]
}
