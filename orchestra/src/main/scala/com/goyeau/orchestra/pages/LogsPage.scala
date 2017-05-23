package com.goyeau.orchestra.pages

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import com.goyeau.orchestra.routes.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._

object LogsPage {
  case class Props(page: TaskLogsPage)(implicit val ec: ExecutionContext)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(String, SetIntervalHandle)](("", null))
      .render { $ =>
        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.pre($.state._1)
        )
      }
      .componentDidMount { $ =>
        implicit val ec = $.props.ec

        def pullLogs($ : ComponentDidMount[Props, (String, SetIntervalHandle), Unit]) = {
          println("Pulling logs")
          $.props.page.job.Api.client
            .logs($.props.page.runId)
            .call()
            .foreach(logs => $.modState(_.copy(_1 = logs)).runNow())
        }

        pullLogs($)
        $.setState($.state.copy(_2 = js.timers.setInterval(1.second)(pullLogs($))))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
      .build
}
