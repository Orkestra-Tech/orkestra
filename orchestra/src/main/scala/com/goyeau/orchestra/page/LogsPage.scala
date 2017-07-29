package com.goyeau.orchestra.page

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import com.goyeau.orchestra.route.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{document, window}

object LogsPage {
  case class Props(page: TaskLogsPage)(implicit val ec: ExecutionContext)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(Seq[String], SetIntervalHandle)]((Seq.empty, null))
      .render { $ =>
        val logs =
          if ($.state._1.nonEmpty) $.state._1.mkString("\n")
          else "No logged message yet"

        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.pre(
            ^.dangerouslySetInnerHtml :=
              newInstance(global.AnsiUp)()
                .ansi_to_html(logs)
                .asInstanceOf[String]
          )
        )
      }
      .componentDidMount { $ =>
        pullLogs($)
        $.setState($.state.copy(_2 = js.timers.setInterval(1.second)(pullLogs($))))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
      .build

  private def pullLogs($ : ComponentDidMount[Props, (Seq[String], SetIntervalHandle), Unit]) = {
    implicit val ec = $.props.ec
    $.props.page.job.Api.client
      .logs($.props.page.runId, $.state._1.size)
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = $.state._1 ++ logs)).runNow()
        if (isScrolledToBottom) window.scrollTo(window.pageXOffset.toInt, document.body.scrollHeight)
      }
  }
}
