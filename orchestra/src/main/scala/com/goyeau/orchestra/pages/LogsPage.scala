package com.goyeau.orchestra.pages

import com.goyeau.orchestra.routes.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object LogsPage {

  def component(itemPage: TaskLogsPage) =
    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .render { $ =>
        <.div(
          <.div("Logs: " + itemPage.runId.toString)
        )
      }
      .build
      .apply()
}
