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
import org.scalajs.dom.{document, window}

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
        pullLogs($)
        $.setState($.state.copy(_3 = js.timers.setInterval(1.second)(pullLogs($))))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._3)))
      .build

  private def pullLogs($ : ComponentDidMount[Props, (String, Int, SetIntervalHandle), Unit]) = {
    implicit val ec = $.props.ec
    $.props.page.job.Api.client
      .logs($.props.page.runId, $.state._2)
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = $.state._1 + logs.mkString("\n"), _2 = $.state._2 + logs.size)).runNow()
        if (isScrolledToBottom) window.scrollTo(0, document.body.scrollHeight)
      }
  }
}
