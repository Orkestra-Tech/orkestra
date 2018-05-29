package com.goyeau.orchestra.board

import com.goyeau.orchestra.route.WebRouter.PageRoute
import japgolly.scalajs.react.extra.router.StaticDsl

trait Board {
  val segment: String
  val name: String
  def route(parentBreadcrumb: Seq[String]): StaticDsl.Rule[PageRoute]
}
