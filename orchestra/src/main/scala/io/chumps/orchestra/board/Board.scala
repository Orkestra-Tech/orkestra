package io.chumps.orchestra.board

import japgolly.scalajs.react.extra.router.StaticDsl
import io.chumps.orchestra.route.WebRouter.PageRoute

trait Board {
  lazy val pathName: String = name.toLowerCase.replaceAll("\\s", "")
  def name: String
  def route: StaticDsl.Rule[PageRoute]
}
