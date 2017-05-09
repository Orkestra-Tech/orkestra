package com.goyeau.orchestra

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import autowire._
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import io.circe.generic.auto._
import org.scalajs.dom.window

sealed trait Board {
  def name: String
  def route(implicit ec: ExecutionContext): StaticDsl.Rule[Board]
  def apiRoute(implicit ec: ExecutionContext): Route
}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    staticRoute(name, this) ~>
      renderR(ctrl => FolderBoardView.component(FolderBoardView.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(name)
  }

  def apiRoute(implicit ec: ExecutionContext) = childBoards.map(_.apiRoute).reduce(_ ~ _)
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}

object FolderBoardView {
  case class Props(name: String, childBoards: Seq[Board], ctrl: RouterCtl[Board])

  val component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .render_P { props =>
        <.div(
          <.div(props.name),
          <.div(props.childBoards.toTagMod(board => <.button(props.ctrl.setOnClick(board), board.name)))
        )
      }
      .build
}

case class SingleTaskBoard[Params <: HList, ParamValues: Encoder, Result: Decoder](
  name: String,
  task: Task[Params, ParamValues, Result]
)(implicit paramGetter: ParamGetter[Params, ParamValues])
    extends Board {

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    staticRoute(name, this) ~> render(SingleTaskBoardView.component(SingleTaskBoardView.Props(name, task)))
  }

  def apiRoute(implicit ec: ExecutionContext) = path(task.id.name / Segments) { segments =>
    post(task.ApiServer.route(segments))
  }
}

object SingleTaskBoardView {
  case class Props[Params <: HList, ParamValues, Result](name: String, task: Task[Params, ParamValues, Result])(
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
        val taskInfo = Task.Info(runId = RunId.newId())
        (taskInfo, Map[String, Any](RunId.name -> taskInfo.runId))
      }
      .render { $ =>
        def runTask = Callback {
          props.task.apiClient.run($.state._1, props.values($.state._2)).call().map {
            case true => window.alert("Running")
            case false => window.alert("Not running")
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
