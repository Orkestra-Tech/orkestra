package com.goyeau.orchestra

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList

sealed trait Board {
  def name: String
  def route: StaticDsl.Rule[Board]
}

case class FolderBoard(name: String)(childBoards: Board*) extends Board {

  val route = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    staticRoute(name, this) ~>
      renderR(ctrl => FolderBoardView.component(FolderBoardView.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(name)
  }
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

case class SingleTaskBoard[Params <: HList, ParamValues](
  name: String,
  task: Task[Params, ParamValues, _]
)(implicit paramGetter: ParamGetter[Params, ParamValues])
    extends Board {

  val route = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    staticRoute(name, this) ~> render(SingleTaskBoardView.component(SingleTaskBoardView.Props(name, task.params)))
  }
}

object SingleTaskBoardView {
  case class Props[Params <: HList, ParamValues](name: String, params: Params)(
    implicit val paramGetter: ParamGetter[Params, ParamValues]
  ) {
    def displays($ : Displayer.State) =
      paramGetter.displays(params, $)

    def values(rawParams: Map[String, Any]) = paramGetter.values(params, rawParams)
  }

  type SingleTaskBoardRenderScope[Params <: HList, ParamValues] =
    RenderScope[SingleTaskBoardView.Props[Params, ParamValues], (Task.Info, Map[String, Any]), Unit]

  def component[Params <: HList, ParamValues] =
    ScalaComponent
      .builder[Props[Params, ParamValues]](getClass.getSimpleName)
      .initialState {
        val taskInfo = Task.Info(runId = RunId.newId())
        (taskInfo, Map[String, Any](RunId.name -> taskInfo.runId))
      }
      .renderP { ($, props) =>
        def runTask = Callback.alert(s"""RunId: ${$.state._2}
                                        |Parameters: ${props.values($.state._2).toString}
                                        |""".stripMargin)
        //Callback(AutowireClient[Api].runTask('ahah).call())

        val displayState = Displayer.State(kv => $.modState(s => s.copy(_2 = s._2 + kv)), key => $.state._2.get(key)) // TODO: Make it better with lenses

        <.div(
          <.div(props.name),
          props.displays(displayState),
          <.button(^.onClick --> runTask, "Run")
        )
      }
      .build

}
