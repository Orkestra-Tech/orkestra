package com.goyeau.orchestra.page

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import com.goyeau.orchestra.{CommonApi, Page}
import com.goyeau.orchestra.route.WebRouter.TaskLogsPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{document, window}
import io.circe.generic.auto._

object LogsPage {
  case class Props(page: TaskLogsPage)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(Option[Seq[(Option[Symbol], String)]], SetIntervalHandle)]((None, null))
      .render { $ =>
        def format(log: String) = newInstance(global.AnsiUp)().ansi_to_html(log).asInstanceOf[String]
        val PrettyDisplayMaxLines = 10000

        val logs = $.state._1 match {
          case Some(log) if log.nonEmpty && log.size <= PrettyDisplayMaxLines =>
            log.zipWithIndex.map {
              case ((stage, line), lineNumber) =>
                <.tr(^.backgroundColor :=? stage.map(s => generateColour(s.name)))(
                  <.td(^.width := "50px", ^.verticalAlign.`text-top`, ^.textAlign.right, ^.paddingRight := "5px")(
                    lineNumber + 1
                  ),
                  <.td(^.width.auto, ^.wordWrap.`break-word`, ^.dangerouslySetInnerHtml := format(line))
                )
            }
          case Some(log) if log.nonEmpty && log.size > PrettyDisplayMaxLines =>
            Seq(
              <.tr(<.td(s"Pretty display disabled as the log is over $PrettyDisplayMaxLines lines")),
              <.tr(<.td(<.pre(^.dangerouslySetInnerHtml := format(log.map(_._2).mkString("\n")))))
            )
          case Some(log) if log.isEmpty => Seq(<.tr(<.td("No logged message yet")))
          case None                     => Seq(<.tr(<.td("Loading log")))
        }

        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.table(^.borderSpacing := "0px", ^.tableLayout.fixed, ^.width := "100%")(
            <.tbody(logs: _*)
          )
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
  ) =
    CommonApi.client
      .logs($.props.page.runId, Page($.state._1.map(_.size), Int.MaxValue))
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = Option($.state._1.toSeq.flatten ++ logs))).runNow()
        if (isScrolledToBottom) window.scrollTo(window.pageXOffset.toInt, document.body.scrollHeight)
      }

  private def generateColour(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
