package io.chumps.orchestra.page

import java.time.Instant

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js

import autowire._

import io.chumps.orchestra._
import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.parameter.State
import io.chumps.orchestra.parameter.ParameterOperations
import io.chumps.orchestra.route.WebRouter.{LogsPageRoute, PageRoute}
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.{ComponentDidMount, RenderScope}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import scalacss.ScalaCssReact._

import io.chumps.orchestra.css.Global
import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.component.StopButton
import io.chumps.orchestra.model.{Page, RunId}
import io.chumps.orchestra.utils.{RunIdOperation, Utils}

object JobPage {

  case class Props[Params <: HList,
                   ParamValuesNoRunId <: HList,
                   ParamValues <: HList: Encoder: Decoder,
                   Result: Decoder](
    job: Job[_, ParamValues, Result],
    params: Params,
    runId: Option[RunId],
    ctl: RouterCtl[PageRoute]
  )(
    implicit paramOperations: ParameterOperations[Params, ParamValuesNoRunId],
    runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues]
  ) {

    def runJob(
      $ : RenderScope[Props[_, _, _ <: HList, _], (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle), Unit]
    )(event: ReactEventFromInput) =
      Callback.future {
        event.preventDefault()
        job.Api.client
          .trigger($.state._1, runIdOperation.inject(paramOperations.values(params, $.state._2), $.state._1))
          .call()
          .map(_ => $.modState(_.copy(_1 = RunId.random(), _2 = Map.empty)))
      }

    def pullHistory(
      $ : ComponentDidMount[Props[_, _, _ <: HList, _], (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle), Unit]
    ) = Callback.future {
      job.Api.client
        .history(Page(None, 50)) // TODO load more as we scroll
        .call()
        .map { runs =>
          val runDisplays = runs.zipWithIndex.toTagMod {
            case ((runId, createdAt, paramValues, tags, runStatus, stageStatuses), index) =>
              val paramsDescription =
                paramOperations
                  .paramsState(params, runIdOperation.remove(paramValues))
                  .map(param => s"${param._1}: ${param._2}")
                  .mkString("\n")
              val rerunButton =
                <.td(^.padding := "0", ^.width := "1px")(
                  <.div(Global.Style.brandColorButton,
                        ^.width := "28px",
                        ^.height := "28px",
                        ^.onClick ==> reRun(paramValues, tags))("↻")
                )
              val stopButton =
                <.td(^.padding := "0", ^.width := "1px")(StopButton.component(StopButton.Props(job, runId)))
              def runIdDisplay(icon: String, runId: RunId, color: String, title: String) =
                TagMod(
                  <.td(Global.Style.tableCell,
                       ^.width := "20px",
                       ^.textAlign.center,
                       ^.backgroundColor := color,
                       ^.title := title)(icon),
                  <.td(Global.Style.tableCell, Global.Style.runId, ^.backgroundColor := color, ^.title := title)(
                    runId.value.toString
                  )
                )
              def datesDisplay(from: Instant, to: Option[Instant]) =
                <.td(Global.Style.tableCell, ^.width.auto, ^.textAlign.center)(
                  s"$createdAt ⟼ ${to.fold("-")(_.toString)}"
                )

              val statusDisplay = runStatus match {
                case Triggered(_) =>
                  <.tr(runIdDisplay("○", runId, Global.Style.brandColor.value, "Triggered"),
                       datesDisplay(createdAt, Option(Instant.now())))
                case Running(_) =>
                  <.tr(runIdDisplay("≻", runId, Global.Style.brandColor.value, "Running"),
                       datesDisplay(createdAt, Option(Instant.now())),
                       stopButton)
                case Success(at, _) =>
                  <.tr(runIdDisplay("✓", runId, "green", "Success"), datesDisplay(createdAt, Option(at)), rerunButton)
                case Failure(at, t) =>
                  <.tr(runIdDisplay("✗", runId, "firebrick", s"Failed: ${t.getMessage}"),
                       datesDisplay(createdAt, Option(at)),
                       rerunButton)
                case Stopped(at) =>
                  <.tr(runIdDisplay("✗", runId, "firebrick", "Stopped"),
                       datesDisplay(createdAt, Option(at).filter(_ != Instant.MAX)),
                       rerunButton)
              }

              <.div(Global.Style.listItem(index % 2 == 0),
                    ^.cursor.pointer,
                    ^.title := paramsDescription,
                    ^.onClick --> $.props.ctl.set(LogsPageRoute(runId)))(
                <.table(^.width := "100%", ^.cellPadding := 0, ^.cellSpacing := 0)(<.tbody(statusDisplay)),
                <.div(
                  stageStatuses
                    .groupBy(_.name)
                    .values
                    .map(statuses => (statuses.head.name, statuses.head.at, statuses.tail.headOption.map(_.at)))
                    .toSeq
                    .sortBy(_._2)
                    .map {
                      case (name, start, end) =>
                        val time =
                          if (runStatus.isInstanceOf[Stopped] && end.isEmpty) ""
                          else s" ${end.getOrElse(Instant.now()).getEpochSecond - start.getEpochSecond}s"

                        <.div(^.padding := "4px",
                              ^.display.`inline-block`,
                              ^.backgroundColor := Utils.generateColour(name))(s"$name$time")
                    }: _*
                )
              )
          }

          $.modState(_.copy(_3 = if (runs.nonEmpty) runDisplays else "No job ran yet"))
        }
    }

    private def reRun(paramValues: ParamValues, tags: Seq[String])(event: ReactEventFromInput) = Callback.future {
      event.stopPropagation()
      job.Api.client.trigger(RunId.random(), paramValues, tags).call().map(Callback(_))
    }

    def displays(
      $ : RenderScope[Props[_, _, _ <: HList, _], (RunId, Map[Symbol, Any], TagMod, SetIntervalHandle), Unit]
    ) = {
      val displayState = State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))
      paramOperations.displays(params, displayState).zipWithIndex.toTagMod {
        case (param, index) => param(Global.Style.listItem(index % 2 == 0))
      }
    }
  }

  val component =
    ScalaComponent
      .builder[Props[_, _, _ <: HList, _]](getClass.getSimpleName)
      .initialStateFromProps[(RunId, Map[Symbol, Any], TagMod, SetIntervalHandle)] { props =>
        val runId = props.runId.getOrElse(RunId.random())
        (runId, Map.empty, "Loading runs", null)
      }
      .renderP { ($, props) =>
        <.main(
          <.h1(props.job.name),
          <.form(^.onSubmit ==> props.runJob($))(
            props.displays($),
            <.button(^.`type` := "submit")("Run")
          ),
          <.h1("History"),
          <.div($.state._3)
        )
      }
      .componentDidMount { $ =>
        $.modState(_.copy(_4 = js.timers.setInterval(1.second)($.props.pullHistory($).runNow())))
          .flatMap(_ => $.props.pullHistory($))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._4)))
      .build
}
