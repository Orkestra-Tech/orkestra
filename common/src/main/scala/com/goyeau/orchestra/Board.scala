package com.goyeau.orchestra

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, StaticDsl}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.{HList, Poly}
import shapeless.ops.hlist.{Mapper, ToTraversable}

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

object GetDisplays extends Poly {
  implicit def forParameter[P <: Parameter[_]](implicit displayer: Displayer[P]) = use((p: P) => displayer.display(p))
}

case class SingleTaskBoard[Params <: HList, Disps <: HList](name: String, task: Task[Params, _])(
  implicit mapper: Mapper.Aux[GetDisplays.type, Params, Disps],
  toSeq: ToTraversable.Aux[Disps, Seq, TagOf[_]]
) extends Board {

  val route = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
    val params = toSeq(task.params.map(GetDisplays))
    staticRoute(name, this) ~> render(SingleTaskBoard.component(SingleTaskBoard.Props(name, params)))
  }
}

object SingleTaskBoard {
  case class Props(name: String, params: Seq[TagOf[_]])

  def component =
    ScalaComponent
      .builder[Props](getClass.getSimpleName)
      .render_P { props =>
        <.div(
          <.div(props.name) +:
            props.params :+
            <.button(^.onClick --> Callback.alert("Toto"), "Run"): _*
        )
      }
      .build
}
