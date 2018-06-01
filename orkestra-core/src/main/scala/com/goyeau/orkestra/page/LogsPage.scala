package com.goyeau.orkestra.page

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import io.circe.generic.auto._
import io.circe.java8.time._

import com.goyeau.orkestra.CommonApi
import com.goyeau.orkestra.route.WebRouter.LogsPageRoute
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.{document, window}

import com.goyeau.orkestra.model.Page
import com.goyeau.orkestra.model.Indexed.LogLine

object LogsPage {
  case class Props(page: LogsPageRoute)

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .initialState[(Option[Seq[LogLine]], SetIntervalHandle)]((None, null))
      .render { $ =>
        def format(log: String) = newInstance(global.AnsiUp)().ansi_to_html(log).asInstanceOf[String]

        val logs = $.state._1 match {
          case Some(log) if log.nonEmpty =>
            <.tr(
              <.td(
                <.pre(^.whiteSpace.`pre-wrap`, ^.dangerouslySetInnerHtml := format(log.map(_.line).mkString("\n")))
              )
            )
          case Some(log) if log.isEmpty => <.tr(<.td("No logged message yet"))
          case None                     => <.tr(<.td("Loading log"))
        }

        <.div(
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
      .logs(
        $.props.page.runId,
        Page($.state._1.flatMap(_.lastOption).map(line => (line.loggedOn, line.position)), 10000)
      )
      .call()
      .foreach { logs =>
        val isScrolledToBottom = window.innerHeight + window.pageYOffset + 1 >= document.body.scrollHeight
        $.modState(_.copy(_1 = Option($.state._1.toSeq.flatten ++ logs))).runNow()
        if (isScrolledToBottom) window.scrollTo(window.pageXOffset.toInt, document.body.scrollHeight)
      }
}
