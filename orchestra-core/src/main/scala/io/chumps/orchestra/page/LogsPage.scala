package io.chumps.orchestra.page

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.CommonApi
import io.chumps.orchestra.route.WebRouter.LogsPageRoute
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{document, window}

import io.chumps.orchestra.model.Page
import io.chumps.orchestra.model.Indexed.LogLine
import io.chumps.orchestra.utils.Utils

object LogsPage {
  case class Props(page: LogsPageRoute)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(Option[Seq[LogLine]], SetIntervalHandle)]((None, null))
      .render { $ =>
        def format(log: String) = newInstance(global.AnsiUp)().ansi_to_html(log).asInstanceOf[String]
        val PrettyDisplayMaxLines = 10000

        val logs = $.state._1 match {
          case Some(log) if log.nonEmpty && log.size <= PrettyDisplayMaxLines =>
            log.zipWithIndex.toTagMod {
              case (logLine, lineNumber) =>
                <.tr(^.backgroundColor :=? logLine.stage.map(s => Utils.generateColour(s.name)))(
                  <.td(^.width := "50px", ^.verticalAlign.`text-top`, ^.textAlign.right, ^.paddingRight := "5px")(
                    lineNumber + 1
                  ),
                  <.td(^.width.auto)(
                    <.pre(^.margin := "0", ^.wordWrap.`break-word`, ^.dangerouslySetInnerHtml := format(logLine.line))
                  )
                )
            }
          case Some(log) if log.nonEmpty && log.size > PrettyDisplayMaxLines =>
            <.tr(
              <.td(
                <.div(s"Pretty display disabled as the log is over $PrettyDisplayMaxLines lines"),
                <.pre(^.wordWrap.`break-word`, ^.dangerouslySetInnerHtml := format(log.map(_.line).mkString("\n")))
              )
            )
          case Some(log) if log.isEmpty => <.tr(<.td("No logged message yet"))
          case None                     => <.tr(<.td("Loading log"))
        }

        <.main(
          <.h1(s"Logs for run ${$.props.page.runId.value}"),
          <.table(^.borderSpacing := "0", ^.tableLayout.fixed, ^.width := "100%")(
            <.tbody(logs)
          )
        )
      }
      .componentDidMount { $ =>
        $.setState($.state.copy(_2 = js.timers.setInterval(1.second)(pullLogs($))))
          .map(_ => pullLogs($))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
      .build

  private def pullLogs($ : ComponentDidMount[Props, (Option[Seq[LogLine]], SetIntervalHandle), Unit]) =
    CommonApi.client
      .logs($.props.page.runId, Page($.state._1.flatMap(_.lastOption).map(_.loggedOn), 10000))
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = Option($.state._1.toSeq.flatten ++ logs))).runNow()
        if (isScrolledToBottom) window.scrollTo(window.pageXOffset.toInt, document.body.scrollHeight)
      }
}
