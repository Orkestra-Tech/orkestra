package com.goyeau.orchestra.pages

import java.util.UUID

import scala.concurrent.ExecutionContext

import autowire._
import com.goyeau.orchestra.routes.WebRouter.{AppPage, TaskLogsPage}
import com.goyeau.orchestra._
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import io.circe.syntax._
import io.circe.generic.auto._
import japgolly.scalajs.react.extra.router.RouterCtl

object SingleTaskBoardPage {

  case class Props[Params <: HList, ParamValues, Result](name: String,
                                                         task: Task[Params, ParamValues, Result],
                                                         ctrl: RouterCtl[AppPage])(
    implicit val paramGetter: ParamGetter[Params, ParamValues]
  ) {
    def displays($ : Displayer.State) = paramGetter.displays(task.params, $)
    def values(rawParams: Map[String, Any]) = paramGetter.values(task.params, rawParams)
  }

  def component[Params <: HList, ParamValues: Encoder, Result: Decoder](
    props: Props[Params, ParamValues, Result]
  )(implicit ec: ExecutionContext) =
    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .initialState {
        val taskInfo = RunInfo(id = UUID.randomUUID())
        (taskInfo, Map[String, Any](RunId.name -> taskInfo.id))
      }
      .render { $ =>
        def runTask = Callback.future {
          props.task.apiClient.run($.state._1, props.values($.state._2)).call().map {
            case RunStatus.Running(_) | RunStatus.Success => props.ctrl.set(TaskLogsPage($.state._1.id))
            case RunStatus.Failed(e) => Callback.alert(e.getMessage)
          }
        }

        val displayState = Displayer.State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key))

        <.div(
          <.div(props.name),
          props.displays(displayState),
          <.button(^.onClick --> runTask, "Run")
        )
      }
      .build
      .apply()
}
