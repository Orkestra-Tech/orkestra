package tech.orkestra.page

import java.time.temporal.ChronoUnit

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js

import autowire._
import tech.orkestra.board.JobBoard
import tech.orkestra.input.State
import tech.orkestra.input.InputOperations
import tech.orkestra.route.WebRouter.{BoardPageRoute, LogsPageRoute, PageRoute}
import io.circe._
import io.circe.generic.auto._
import io.circe.java8.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.{ComponentDidMount, RenderScope}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import scalacss.ScalaCssReact._
import tech.orkestra.css.Global
import tech.orkestra.component.StopButton
import tech.orkestra.model.{Page, RunId}
import tech.orkestra.utils.Colours

object JobPage {

  case class Props[
    Inputs <: HList,
    ParametersNoRunId <: HList,
    Parameters <: HList: Encoder: Decoder,
    Result: Decoder
  ](
    job: JobBoard[Parameters],
    inputs: Inputs,
    page: BoardPageRoute,
    ctl: RouterCtl[PageRoute]
  )(implicit inputOperations: InputOperations[Inputs, Parameters]) {

    def runJob(
      $ : RenderScope[Props[_, _, _ <: HList, _], (RunId, Map[Symbol, Any], TagMod, Option[SetIntervalHandle]), Unit]
    )(event: ReactEventFromInput) =
      Callback.future {
        event.preventDefault()
        job.Api.client
          .trigger($.state._1, inputOperations.values(inputs, $.state._2))
          .call()
          .map(_ => $.modState(_.copy(_1 = RunId.random(), _2 = Map.empty)))
      }

    def pullHistory(
      $ : ComponentDidMount[
        Props[_, _, _ <: HList, _],
        (RunId, Map[Symbol, Any], TagMod, Option[SetIntervalHandle]),
        Unit
      ]
    ) = Callback.future {
      job.Api.client
        .history(Page(None, -50)) // TODO load more as we scroll
        .call()
        .map { history =>
          val runDisplays = history.runs.zipWithIndex.toTagMod {
            case ((run, stages), index) =>
              val paramsDescription =
                inputOperations
                  .inputsState(inputs, run.parameters)
                  .map(input => s"${input._1}: ${input._2}")
                  .mkString("\n")
              val rerunButton =
                <.div(
                  Global.Style.brandColorButton,
                  ^.width := "30px",
                  ^.height := "30px",
                  ^.onClick ==> reRun(run.parameters, run.tags)
                )("↻")
              val stopButton = StopButton.component(StopButton.Props(job, run.runInfo.runId))
              def runIdDisplay(icon: String, runId: RunId, color: String, title: String) =
                TagMod(
                  <.div(
                    Global.Style.cell,
                    ^.width := "20px",
                    ^.justifyContent.center,
                    ^.backgroundColor := color,
                    ^.title := title
                  )(icon),
                  <.div(Global.Style.cell, Global.Style.runId, ^.backgroundColor := color, ^.title := title)(
                    runId.value.toString
                  )
                )
              val datesDisplay =
                <.div(Global.Style.cell, ^.flexGrow := "1", ^.justifyContent.center)(
                  s"${run.triggeredOn} ${run.triggeredOn.until(run.latestUpdateOn, ChronoUnit.SECONDS)}s"
                )

              val statusDisplay = run.result match {
                case None if run.triggeredOn == run.latestUpdateOn =>
                  TagMod(
                    runIdDisplay("○", run.runInfo.runId, Global.Style.brandKubernetesColor.value, "Triggered"),
                    datesDisplay,
                    stopButton
                  )
                case None if run.latestUpdateOn.isBefore(history.updatedOn.minus(10, ChronoUnit.SECONDS)) =>
                  TagMod(runIdDisplay("✗", run.runInfo.runId, "dimgrey", "Stopped"), datesDisplay, rerunButton)
                case None =>
                  TagMod(
                    runIdDisplay("≻", run.runInfo.runId, Global.Style.brandKubernetesColor.value, "Running"),
                    datesDisplay,
                    stopButton
                  )
                case Some(Right(_)) =>
                  TagMod(runIdDisplay("✓", run.runInfo.runId, "green", "Success"), datesDisplay, rerunButton)
                case Some(Left(t)) =>
                  TagMod(
                    runIdDisplay("✗", run.runInfo.runId, "firebrick", s"Failed: ${t.getMessage}"),
                    datesDisplay,
                    rerunButton
                  )
              }

              <.div(
                Global.Style.listItem(index % 2 == 0),
                ^.cursor.pointer,
                ^.title := paramsDescription,
                ^.onClick --> $.props.ctl.set(LogsPageRoute(page.breadcrumb :+ job.name, run.runInfo.runId))
              )(
                <.div(^.display.flex)(statusDisplay),
                <.div(
                  stages.toTagMod { stage =>
                    <.div(
                      ^.padding := "4px",
                      ^.display.`inline-block`,
                      ^.backgroundColor := Colours.generate(stage.name),
                      ^.opacity := (if (stage.runInfo.jobId == job.id ||
                                        stage.parentJob.exists(_.jobId == job.id)) "1"
                                    else "0.5")
                    )(s"${stage.name} ${stage.startedOn.until(stage.latestUpdateOn, ChronoUnit.SECONDS)}s")
                  }
                )
              )
          }

          $.modState(_.copy(_3 = if (history.runs.nonEmpty) runDisplays else "No job ran yet"))
        }
    }

    private def reRun(parameters: Parameters, tags: Seq[String])(event: ReactEventFromInput) = Callback.future {
      event.stopPropagation()
      job.Api.client.trigger(RunId.random(), parameters, tags).call().map(Callback(_))
    }

    def displays(
      $ : RenderScope[Props[_, _, _ <: HList, _], (RunId, Map[Symbol, Any], TagMod, Option[SetIntervalHandle]), Unit]
    ) = {
      val displayState = State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))
      inputOperations.displays(inputs, displayState).zipWithIndex.toTagMod {
        case (param, index) => param(Global.Style.listItem(index % 2 == 0))
      }
    }
  }

  val component =
    ScalaComponent
      .builder[Props[_, _, _ <: HList, _]](getClass.getSimpleName)
      .initialStateFromProps[(RunId, Map[Symbol, Any], TagMod, Option[SetIntervalHandle])] { props =>
        val runId = props.page.runId.getOrElse(RunId.random())
        (runId, Map.empty, "Loading runs", None)
      }
      .renderP { ($, props) =>
        <.div(
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
        $.modState(_.copy(_4 = Option(js.timers.setInterval(1.second)($.props.pullHistory($).runNow()))))
          .flatMap(_ => $.props.pullHistory($))
      }
      .componentWillUnmount($ => Callback($.state._4.foreach(js.timers.clearInterval)))
      .build
}
