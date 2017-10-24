package io.chumps.orchestra.component

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import io.chumps.orchestra.CommonApi
import io.circe.generic.auto._

import io.chumps.orchestra.css.Global
import io.chumps.orchestra.model.RunInfo

object RunningJobs {

  val component = ScalaComponent
    .builder[Unit](getClass.getSimpleName)
    .initialState[(Boolean, Option[Seq[RunInfo]])]((false, None))
    .render { $ =>
      def stop(runInfo: RunInfo) =
        Callback.future(runInfo.job.Api.client.stop(runInfo.runId).call().map(Callback(_)))

      val runningJobsDisplay = {
        val runs = $.state._2 match {
          case Some(runningJobs) if runningJobs.nonEmpty =>
            runningJobs.zipWithIndex.map {
              case (runInfo, index) =>
                <.tr(Global.Style.listItem(index % 2 == 0))(
                  <.td(runInfo.job.name),
                  <.td(runInfo.runId.value.toString),
                  <.td(<.button(^.onClick --> stop(runInfo))("X"))
                )
            }
          case Some(runningJobs) if runningJobs.isEmpty => Seq(<.tr(<.td("No running jobs")))
          case None                                     => Seq(<.tr(<.td("Loading running jobs")))
        }

        <.table(^.position.absolute, ^.right := "0", ^.whiteSpace.nowrap, ^.backgroundColor := "green")(
          <.tbody(runs: _*)
        )
      }

      <.li(^.float.right,
           ^.position.relative,
           ^.tabIndex := 0,
           ^.outline := "none",
           ^.onBlur --> $.modState(_.copy(_1 = false)))(
        <.div(
          TopNav.Style.menuItem($.state._1),
          ^.onClick --> $.modState(_.copy(_1 = ! $.state._1)).flatMap(_ => pullRunningJobs($))
        )("Running Jobs"),
        if ($.state._1) runningJobsDisplay else TagMod()
      )
    }
    .build

  private def pullRunningJobs($ : RenderScope[Unit, (Boolean, Option[Seq[RunInfo]]), Unit]) =
    Callback.future {
      CommonApi.client.runningJobs().call().map { runningJobs =>
        $.modState(_.copy(_2 = Option(runningJobs)))
      }
    }
}
