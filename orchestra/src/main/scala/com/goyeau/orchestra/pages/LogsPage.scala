package com.goyeau.orchestra.pages

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.scalajs.js

import autowire._
import com.goyeau.orchestra.Job
import com.goyeau.orchestra.routes.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

object LogsPage {
  case class Props(page: TaskLogsPage, job: Job.Definition[_, _ <: HList])(implicit val ec: ExecutionContext)

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
        implicit val ec = $.props.ec

        def pullLogs($ : ComponentDidMount[Props, String, Unit]) =
          $.props.job.Api.client.logs($.props.page.runId).call().foreach(logs => $.modState(_ => logs).runNow())

        Callback {
          pullLogs($)
          js.timers.setInterval(1.second)(pullLogs($))
        }
      }
      .build
}
