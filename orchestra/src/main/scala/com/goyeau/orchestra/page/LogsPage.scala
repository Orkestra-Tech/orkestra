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
import io.circe.generic.auto._

object LogsPage {
  case class Props(page: TaskLogsPage)(implicit val ec: ExecutionContext)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(Option[Seq[(Option[Symbol], String)]], SetIntervalHandle)]((None, null))
      .render { $ =>
        val logs = $.state._1 match {
          case Some(log) if log.nonEmpty => log.map(_._2)
          case Some(log) if log.isEmpty  => Seq("No logged message yet")
          case None                      => Seq("Loading log")
        }

        val ansiUp = newInstance(global.AnsiUp)()
        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.div(logs.map { line =>
            <.pre(
              ^.margin := "0px",
              ^.dangerouslySetInnerHtml :=
                ansiUp.ansi_to_html(line).asInstanceOf[String]
            )
          }: _*)
        )
      }
      .componentDidMount { $ =>
        $.setState($.state.copy(_2 = js.timers.setInterval(1.second)(pullLogs($))))
          .map(_ => pullLogs($))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
      .build

  private def pullLogs(
    $ : ComponentDidMount[Props, (Option[Seq[(Option[Symbol], String)]], SetIntervalHandle), Unit]
  ) = {
    implicit val ec = $.props.ec
    $.props.page.job.Api.client
      .logs($.props.page.runId, $.state._1.fold(0)(_.size))
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = Option($.state._1.toSeq.flatten ++ logs))).runNow()
        if (isScrolledToBottom) window.scrollTo(window.pageXOffset.toInt, document.body.scrollHeight)
      }
  }
}
