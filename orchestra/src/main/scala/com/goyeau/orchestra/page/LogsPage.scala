package com.goyeau.orchestra.page

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.macros.whitebox
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
        val ansiUp = newInstance(global.AnsiUp)()
        val logs = $.state._1 match {
          case Some(log) if log.nonEmpty =>
            log.zipWithIndex.map {
              case ((stage, line), lineNumber) =>
                <.tr(^.backgroundColor :=? stage.map(s => generateColour(s.name)))(
                  <.td(lineNumber + 1),
                  <.td(^.wordWrap.`break-word`)(
                    <.pre(
                      ^.margin := "0px",
                      ^.dangerouslySetInnerHtml :=
                        ansiUp.ansi_to_html(line).asInstanceOf[String]
                    )
                  )
                )
            }
          case Some(log) if log.isEmpty => Seq(<.tr(<.td("No logged message yet")))
          case None                     => Seq(<.tr(<.td("Loading log")))
        }

        <.div(
          <.div("Logs: " + $.props.page.runId.toString),
          <.table(^.borderSpacing := "0px")(
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

  private def generateColour(s: String): String = {
    val i = s.hashCode
    "#" +
      Integer.toHexString((i >> 24) & 0xFF) +
      Integer.toHexString((i >> 16) & 0xFF) +
      Integer.toHexString((i >> 8) & 0xFF) +
      Integer.toHexString(i & 0xFF)
  }
}
