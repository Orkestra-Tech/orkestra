package io.chumps.orchestra.page

import java.util.UUID

import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js

import autowire._
import io.chumps.orchestra._
import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.parameter.Parameter.State
import io.chumps.orchestra.parameter.{ParameterOperations, RunId}
import io.chumps.orchestra.route.WebRouter.{LogsPageRoute, PageRoute}
import io.circe._
import io.circe.java8.time._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.{ComponentDidMount, RenderScope}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

object JobBoardPage {
  case class Props[Params <: HList, ParamValues <: HList](
    name: String,
    job: Job.Definition[_, ParamValues, _],
    params: Params,
    ctl: RouterCtl[PageRoute]
  )(
    implicit paramOperations: ParameterOperations[Params, ParamValues],
    encoder: Encoder[ParamValues]
  ) {

    def runJob(state: (UUID, Map[Symbol, Any], Seq[TagMod], SetIntervalHandle))(event: ReactEventFromInput) =
      Callback.future {
        event.preventDefault()
        job.Api.client.trigger(state._1, paramOperations.values(params, state._2)).call().map(Callback(_))
      }

    def displays(
      $ : RenderScope[Props[_, _ <: HList], (UUID, Map[Symbol, Any], Seq[TagMod], SetIntervalHandle), Unit]
    ) = {
      val displayState = State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))
      paramOperations.displays(params, displayState)
    }
  }

  val component =
    ScalaComponent
      .builder[Props[_, _ <: HList]](getClass.getSimpleName)
      .initialStateFromProps[(UUID, Map[Symbol, Any], Seq[TagMod], SetIntervalHandle)] { props =>
        val runId = UUID.randomUUID()
        (runId, Map(RunId.id -> runId), Seq(<.tr(<.td("Loading runs"))), null)
      }
      .renderP { ($, props) =>
        <.div(
          <.div(props.name),
          <.form(^.onSubmit ==> props.runJob($.state))(
            props.displays($) :+
              <.button(^.`type` := "submit")("Run"): _*
          ),
          <.div("History"),
          <.table(<.tbody($.state._3: _*))
        )
      }
      .componentDidMount { $ =>
        $.setState($.state.copy(_4 = js.timers.setInterval(1.second)(pullRuns($).runNow())))
          .flatMap(_ => pullRuns($))
      }
      .componentWillUnmount($ => Callback(js.timers.clearInterval($.state._4)))
      .build

  def pullRuns(
    $ : ComponentDidMount[Props[_, _ <: HList], (UUID, Map[Symbol, Any], Seq[TagMod], SetIntervalHandle), Unit]
  ) = Callback.future {
    $.props.job.Api.client
      .runs(Page(None, 50)) // TODO load more as we scroll
      .call()
      .map { runs =>
        val runDisplays = runs.map {
          case (uuid, createdAt, runStatus) =>
            val statusDisplay = runStatus match {
              case _: Triggered    => <.td("Triggered")
              case _: Running      => <.td(<.button(^.onClick --> stop($.props.job, uuid))("X"))
              case _: Success      => <.td("Success")
              case _: Failure      => <.td("Failure")
              case _: Stopped.type => <.td("Stopped")
            }

            <.tr(
              <.td(<.button(^.onClick --> $.props.ctl.set(LogsPageRoute(uuid)))(uuid.toString)),
              <.td(createdAt.toString),
              statusDisplay
            )
        }
        $.modState(_.copy(_3 = if (runDisplays.nonEmpty) runDisplays else Seq(<.tr(<.td("No job ran yet")))))
      }
  }

  def stop(job: Job.Definition[_, _, _], runId: UUID) =
    Callback.future(job.Api.client.stop(runId).call().map(Callback(_)))
}
