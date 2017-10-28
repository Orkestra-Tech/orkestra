package io.chumps.orchestra.component

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import io.circe.generic.auto._
import shapeless.HList

import io.chumps.orchestra.CommonApi
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.css.Global
import io.chumps.orchestra.model.RunInfo

object RunningJobs {

  val component = ScalaComponent
    .builder[Seq[Job[_, _ <: HList]]](getClass.getSimpleName)
    .initialState[(Option[Seq[RunInfo]], SetIntervalHandle)]((None, null))
    .renderP { ($, jobs) =>
      val runs = $.state._1 match {
        case Some(runningJobs) if runningJobs.nonEmpty =>
          runningJobs.zipWithIndex.toTagMod {
            case (runInfo, index) =>
              val job = jobs
                .find(_.id == runInfo.jobId)
                .getOrElse(throw new IllegalStateException(s"Can't find the job with id ${runInfo.jobId}"))

              <.tr(Global.Style.listItem(index % 2 == 0))(
                <.td(Global.Style.tableCell, ^.overflow.hidden)(job.name),
                <.td(Global.Style.tableCell, Global.Style.runId)(runInfo.runId.value.toString),
                <.td(^.padding := "0", ^.width := "1px")(StopButton.component(StopButton.Props(job, runInfo.runId)))
              )
          }
        case Some(runningJobs) if runningJobs.isEmpty => <.tr(<.td(Global.Style.tableCell)("No running jobs"))
        case None                                     => <.tr(<.td(Global.Style.tableCell)("Loading running jobs"))
      }

      <.table(
        ^.position.absolute,
        ^.right := "0",
        ^.width := "600px",
        ^.cellPadding := 0,
        ^.cellSpacing := 0,
        ^.backgroundColor := Global.Style.brandColor.value,
        ^.boxShadow := "inset 0 0 10000px rgba(0, 0, 0, 0.06)"
      )(<.tbody(runs))
    }
    .componentDidMount { $ =>
      $.modState(_.copy(_2 = js.timers.setInterval(1.second)(pullRunningJobs($).runNow())))
        .flatMap(_ => pullRunningJobs($))
    }
    .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
    .build

  private def pullRunningJobs(
    $ : ComponentDidMount[Seq[Job[_, _ <: HList]], (Option[Seq[RunInfo]], SetIntervalHandle), Unit]
  ) =
    Callback.future {
      CommonApi.client.runningJobs().call().map { runningJobs =>
        $.modState(_.copy(_1 = Option(runningJobs)))
      }
    }
}
