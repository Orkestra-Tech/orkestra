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
      .initialState[(String, Int, SetIntervalHandle)](("", 0, null))
      .render { $ =>
        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.pre($.state._1)
        )
      }
      .componentDidMount { $ =>
        implicit val ec = $.props.ec

        def pullLogs($ : ComponentDidMount[Props, (String, Int, SetIntervalHandle), Unit]) =
          $.props.page.job.Api.client
            .logs($.props.page.runId, $.state._2)
            .call()
            .foreach { logs =>
              $.modState(_.copy(_1 = $.state._1 + logs.mkString("\n"), _2 = $.state._2 + logs.size)).runNow()
            }

        pullLogs($)
        $.setState($.state.copy(_3 = js.timers.setInterval(1.second)(pullLogs($))))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._3)))
      .build
}
