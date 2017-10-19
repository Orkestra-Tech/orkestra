package io.chumps.orchestra.page

import java.time.Instant

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js

import autowire._

import io.chumps.orchestra._
import io.chumps.orchestra.BaseEncoders._
import io.chumps.orchestra.parameter.Parameter.State
import io.chumps.orchestra.parameter.{JobRunId, ParameterOperations}
import io.chumps.orchestra.route.WebRouter.{LogsPageRoute, PageRoute}
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.{ComponentDidMount, RenderScope}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.ScalaCssReact._

import io.chumps.orchestra.css.Global
import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.model.{Page, RunId}

object JobBoardPage {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val item = style(
      padding(4.px),
      display.inlineBlock
    )
  }

  case class Props[Params <: HList, ParamValues <: HList](
    job: Job.Definition[_, ParamValues, _],
    params: Params,
    runId: Option[RunId],
    ctl: RouterCtl[PageRoute]
  )(
    implicit paramOperations: ParameterOperations[Params, ParamValues],
    encoder: Encoder[ParamValues]
  ) {

    def runJob(state: (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle))(event: ReactEventFromInput) =
      Callback.future {
        event.preventDefault()
        job.Api.client.trigger(state._1, paramOperations.values(params, state._2)).call().map(Callback(_))
      }

    def displays(
      $ : RenderScope[Props[_, _ <: HList], (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle), Unit]
    ) = {
      val displayState = State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))
      paramOperations.displays(params, displayState).zipWithIndex.toTagMod {
        case (param, index) => param(Global.Style.listItem(index % 2 == 0))
      }
    }
  }

  val component =
    ScalaComponent
      .builder[Props[_, _ <: HList]](getClass.getSimpleName)
      .initialStateFromProps[(RunId, Map[Symbol, Any], TagMod, SetIntervalHandle)] { props =>
        val runId = props.runId.getOrElse(RunId.random())
        (runId, Map(JobRunId.id -> runId), "Loading runs", null)
      }
      .renderP { ($, props) =>
        <.div(
          <.h1(props.job.name),
          <.form(^.onSubmit ==> props.runJob($.state))(
            props.displays($),
            <.button(^.`type` := "submit")("Run")
          ),
          <.h1("History"),
          <.div($.state._3)
        )
      }
      .componentDidMount { $ =>
        $.setState($.state.copy(_4 = js.timers.setInterval(1.second)(pullRuns($).runNow())))
          .flatMap(_ => pullRuns($))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._4)))
      .build

  private def pullRuns(
    $ : ComponentDidMount[Props[_, _ <: HList], (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle), Unit]
  ) = Callback.future {
    $.props.job.Api.client
      .runs(Page(None, 50)) // TODO load more as we scroll
      .call()
      .map { runs =>
        val runDisplays = runs.zipWithIndex.toTagMod {
          case ((runId, createdAt, runStatus, stageStatuses), index) =>
            val statusDisplay = runStatus match {
              case _: Triggered    => <.div(Style.item)("Triggered")
              case _: Running      => <.button(^.onClick ==> stop($.props.job, runId))("X")
              case _: Success      => <.span(Style.item)("Success")
              case _: Failure      => <.span(Style.item)("Failure")
              case _: Stopped.type => <.span(Style.item)("Stopped")
            }

            <.div(Global.Style.listItem(index % 2 == 0),
                  ^.cursor.pointer,
                  ^.onClick --> $.props.ctl.set(LogsPageRoute(runId)))(
              <.div(
                <.span(Style.item, ^.backgroundColor := Global.Style.brandColor.value)(runId.toString),
                <.span(Style.item)(createdAt.toString),
                statusDisplay
              ),
              <.div(
                stageStatuses
                  .groupBy(_.name)
                  .map {
                    case (name, statuses) if statuses.size == 2 =>
                      <.span(Style.item, ^.backgroundColor := Utils.generateColour(name))(
                        s"$name ${statuses.last.at.getEpochSecond - statuses.head.at.getEpochSecond}s"
                      )
                    case (name, statuses) =>
                      <.span(Style.item, ^.backgroundColor := Utils.generateColour(name))(
                        s"$name ${Instant.now().getEpochSecond - statuses.head.at.getEpochSecond}s"
                      )
                  }
                  .toSeq: _*
              )
            )
        }

        $.modState(_.copy(_3 = if (runs.nonEmpty) runDisplays else "No job ran yet"))
      }
  }

  private def stop(job: Job.Definition[_, _, _], runId: RunId)(event: ReactEventFromInput) = Callback.future {
    event.stopPropagation()
    job.Api.client.stop(runId).call().map(Callback(_))
  }
}
