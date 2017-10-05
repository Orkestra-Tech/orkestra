package com.goyeau.orchestra.model

import com.goyeau.orchestra.route.WebRouter.AppPage
import japgolly.scalajs.react.Callback

case class PageMenu(name: String, route: AppPage)
case class ActionMenu(name: String, action: Callback)
