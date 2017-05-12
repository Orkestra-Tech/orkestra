package com.goyeau.orchestra.pages

import scala.concurrent.ExecutionContext

import autowire._
import com.goyeau.orchestra.Task
import com.goyeau.orchestra.routes.WebRouter.TaskLogsPage
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import scala.scalajs.js
import scala.concurrent.duration._

import japgolly.scalajs.react.component.builder.Lifecycle.StateRW

object LogsPage {

  def component[Params <: HList, ParamValues: Encoder, Result: Decoder](
    itemPage: TaskLogsPage,
    task: Task[Params, ParamValues, Result]
  )(implicit ec: ExecutionContext) = {
    def pullLogs($ : StateRW[Unit, String, Unit]) =
      task.apiClient.logs(itemPage.runId).call().foreach(logs => $.modState(_ => logs).runNow())

    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .initialState[String]("")
      .render { $ =>
        <.div(
          <.div("Logs: " + itemPage.runId.toString),
          <.div($.state)
        )
      }
      .componentDidMount { $ =>
        Callback {
          pullLogs($)
          js.timers.setInterval(1.second)(pullLogs($))
        }
      }
      .build
      .apply()
  }
}
