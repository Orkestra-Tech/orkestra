package com.drivetribe.orchestra.component

import java.time.temporal.ChronoUnit

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
import io.circe.shapes._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.Style
import shapeless.HList
import com.drivetribe.orchestra.CommonApi
import com.drivetribe.orchestra.board.JobBoard
import com.drivetribe.orchestra.css.Global
import com.drivetribe.orchestra.model.Indexed._
import com.drivetribe.orchestra.route.WebRouter.{BoardPageRoute, LogsPageRoute, PageRoute}

object RunningJobs {

  case class Props(ctl: RouterCtl[PageRoute], jobs: Seq[JobBoard[_ <: HList, _, _, _]], closeRunningJobs: Callback)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[(Option[Seq[Run[_, _]]], SetIntervalHandle)]((None, null))
    .renderP { ($, props) =>
      val runs = $.state._1 match {
        case Some(runningJobs) if runningJobs.nonEmpty =>
          runningJobs.toTagMod { run =>
            val job = props.jobs
              .find(_.id == run.runInfo.jobId)
              .getOrElse(throw new IllegalStateException(s"Can't find the job with id ${run.runInfo.jobId}"))

            TagMod(
              <.div(
                Global.Style.cell,
                ^.overflow.hidden,
                TopNav.Style.clickableItem(false),
                ^.onClick --> props.ctl.set(BoardPageRoute(Seq.empty, job)).flatMap(_ => props.closeRunningJobs),
              )(job.name),
              <.div(
                Global.Style.cell,
                Global.Style.runId,
                TopNav.Style.clickableItem(false),
                ^.onClick --> props.ctl
                  .set(LogsPageRoute(Seq.empty, run.runInfo.runId))
                  .flatMap(_ => props.closeRunningJobs)
              )(run.runInfo.runId.value.toString),
              <.div(Global.Style.cell)(s"${run.triggeredOn.until(run.latestUpdateOn, ChronoUnit.SECONDS)}s"),
              StopButton.component(StopButton.Props(job, run.runInfo.runId))
            )
          }
        case Some(runningJobs) if runningJobs.isEmpty => <.div(Global.Style.cell)("No running jobs")
        case None                                     => <.div(Global.Style.cell)("Loading running jobs")
      }

      <.div(
        ^.display := "grid",
        Style("grid-template-columns") := "1fr 0fr 0fr 0fr",
        ^.position.absolute,
        ^.right := "0",
        ^.width := "680px",
        ^.backgroundColor := Global.Style.brandKubernetesColor.value,
        ^.boxShadow := "inset 0 0 10000px rgba(0, 0, 0, 0.06)"
      )(runs)
    }
    .componentDidMount { $ =>
      $.modState(_.copy(_2 = js.timers.setInterval(1.second)(pullRunningJobs($).runNow())))
        .flatMap(_ => pullRunningJobs($))
    }
    .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._2)))
    .build

  private def pullRunningJobs(
    $ : ComponentDidMount[Props, (Option[Seq[Run[_, _]]], SetIntervalHandle), Unit]
  ) = Callback.future {
    CommonApi.client.runningJobs().call().map(runningJobs => $.modState(_.copy(_1 = Option(runningJobs))))
  }
}
