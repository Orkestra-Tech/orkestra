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
import japgolly.scalajs.react.extra.router.RouterCtl
import shapeless.HList

import io.chumps.orchestra.CommonApi
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.css.Global
import io.chumps.orchestra.model.RunInfo
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, LogsPageRoute, PageRoute}

object RunningJobs {

  case class Props(ctl: RouterCtl[PageRoute], jobs: Seq[Job[_ <: HList, _, _, _]], closeRunningJobs: Callback)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[(Option[Seq[RunInfo]], SetIntervalHandle)]((None, null))
    .renderP { ($, props) =>
      val runs = $.state._1 match {
        case Some(runningJobs) if runningJobs.nonEmpty =>
          runningJobs.zipWithIndex.toTagMod {
            case (runInfo, index) =>
              val job = props.jobs
                .find(_.id == runInfo.jobId)
                .getOrElse(throw new IllegalStateException(s"Can't find the job with id ${runInfo.jobId}"))

              <.div(^.display.flex, Global.Style.listItem(index % 2 == 0))(
                <.div(
                  Global.Style.cell,
                  ^.flexGrow := "1",
                  ^.overflow.hidden,
                  TopNav.Style.clickableItem(false),
                  ^.onClick --> props.ctl.set(BoardPageRoute(Seq.empty, job)).flatMap(_ => props.closeRunningJobs),
                )(job.name),
                <.div(
                  Global.Style.cell,
                  Global.Style.runId,
                  TopNav.Style.clickableItem(false),
                  ^.onClick --> props.ctl
                    .set(LogsPageRoute(Seq.empty, runInfo.runId))
                    .flatMap(_ => props.closeRunningJobs)
                )(runInfo.runId.value.toString),
                StopButton.component(StopButton.Props(job, runInfo.runId))
              )
          }
        case Some(runningJobs) if runningJobs.isEmpty => <.div(Global.Style.cell)("No running jobs")
        case None                                     => <.div(Global.Style.cell)("Loading running jobs")
      }

      <.div(
        ^.position.absolute,
        ^.right := "0",
        ^.width := "600px",
        ^.backgroundColor := Global.Style.brandColor.value,
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
    $ : ComponentDidMount[Props, (Option[Seq[RunInfo]], SetIntervalHandle), Unit]
  ) =
    Callback.future {
      CommonApi.client.runningJobs().call().map { runningJobs =>
        $.modState(_.copy(_1 = Option(runningJobs)))
      }
    }
}
