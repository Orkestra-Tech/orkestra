package com.goyeau.orchestra.pages

import java.util.UUID

import scala.concurrent.ExecutionContext

import autowire._
import com.goyeau.orchestra._
import com.goyeau.orchestra.ARunStatus._
import com.goyeau.orchestra.routes.WebRouter.{AppPage, TaskLogsPage}
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

object SingleJobBoardPage {

  def component[Params <: HList, ParamValues <: HList: Encoder, Result: Decoder](
    name: String,
    job: Job.Definition[_, ParamValues, Result],
    params: Params,
    ctrl: RouterCtl[AppPage]
  )(implicit ec: ExecutionContext, paramGetter: ParameterGetter[Params, ParamValues]) =
    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .initialState {
        val jobInfo = RunInfo(job.id, Option(UUID.randomUUID()))
        (jobInfo, Map[String, Any](RunId.name -> jobInfo.runId), <.div("Loading runs"))
      }
      .render { $ =>
        def runJob = Callback.future {
          job.Api.client.run($.state._1, paramGetter.values(params, $.state._2)).call().map {
            case ARunStatus.Failure(e) => Callback.alert(e.getMessage)
            case _ => ctrl.set(TaskLogsPage(job, $.state._1.runId))
          }
        }

        val displayState =
          ParameterDisplayer.State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))

        <.div(
          <.div(name),
          <.div(paramGetter.displays(params, displayState): _*),
          <.button(^.onClick --> runJob, "Run"),
          $.state._3
        )
      }
      .componentDidMount { $ =>
        Callback.future(
          job.Api.client
            .runs()
            .call()
            .map { runs =>
              val runDisplays =
                runs.map(uuid => <.div(<.button(^.onClick --> ctrl.set(TaskLogsPage(job, uuid)), uuid.toString)))
              $.modState(_.copy(_3 = if (runDisplays.nonEmpty) <.div(runDisplays: _*) else <.div("No job ran yet")))
            }
        )
      }
      .build
      .apply()
}
