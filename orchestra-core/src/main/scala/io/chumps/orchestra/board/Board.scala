package io.chumps.orchestra.board

import japgolly.scalajs.react.extra.router.StaticDsl

import io.chumps.orchestra.route.WebRouter.PageRoute

trait Board {
  val segment: String
  val name: String
  def route(parentBreadcrumb: Seq[String]): StaticDsl.Rule[PageRoute]
}
