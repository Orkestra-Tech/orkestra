package io.chumps.orchestra.model

import io.chumps.orchestra.route.WebRouter.AppPage
import japgolly.scalajs.react.Callback

case class PageMenu(name: String, route: AppPage)
case class ActionMenu(name: String, action: Callback)
