package com.goyeau.orchestra

import japgolly.scalajs.react._
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
    staticRoute(name, this) ~> renderR(ctrl => FolderBoard.component(FolderBoard.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(name)
  }
}

object FolderBoard {
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

case class SingleTaskBoard[Params <: HList, ParamValue](
  name: String,
  task: Task[Params, ParamValue, _]
)(implicit paramGetter: ParamGetter[Params, ParamValue])
    extends Board {

  val route = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    staticRoute(name, this) ~> render(component(name, task.params).apply)
  }

  def component(name: String, params: Params) =
    ScalaComponent
      .builder[Unit](getClass.getSimpleName)
      .initialState(Map.empty[String, Any])
      .render { $ =>
        def runTask = Callback.alert(paramGetter.values(params, $.state).toString)
        //Callback(AutowireClient[Api].runTask('ahah).call())

        <.div(
          <.div(name),
          paramGetter.displays(params, $),
          <.button(^.onClick --> runTask, "Run")
        )
      }
      .build
}

//object SingleTaskBoard {
//  case class Props(name: String, params: Seq[TagOf[_]])
//
//  def component =
//    ScalaComponent
//      .builder[Props](getClass.getSimpleName)
//      .render_P { props =>
//        <.div(
//          <.div(props.name) +:
//            props.params :+
//            <.button(^.onClick --> runTask, "Run"): _*
//        )
//      }
//      .build
//
//  def runTask = Callback(AutowireClient[Api].runTask('ahah).call())
//}
