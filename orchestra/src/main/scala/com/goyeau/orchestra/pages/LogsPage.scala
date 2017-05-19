package com.goyeau.orchestra.pages

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

import autowire._
import com.goyeau.orchestra.Task
import com.goyeau.orchestra.routes.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

object LogsPage {
  case class Props(page: TaskLogsPage, task: Task.Definition[_, _ <: HList])

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[String]("")
      .render { $ =>
        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.div($.state)
        )
      }
      .componentDidMount { $ =>
        def pullLogs($ : ComponentDidMount[Props, String, Unit]) =
          $.props.task.Api.client.logs($.props.page.runId).call().foreach(logs => $.modState(_ => logs).runNow())

        Callback {
          pullLogs($)
          js.timers.setInterval(1.second)(pullLogs($))
        }
      }
      .build
}
