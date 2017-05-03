package com.goyeau.orchestra

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import com.sun.xml.internal.ws.wsdl.writer.document.ParamType
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, RouterCtl, StaticDsl}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.{HList, HNil, Poly}
import shapeless.ops.hlist.{LeftFolder, Mapper, ToTraversable}
import io.circe.generic.auto._
import shapeless.ops.tuple.Prepend
import shapeless.::

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

//object GetDisplays extends Poly {
//  implicit def forParameter[P <: Parameter[_]](implicit displayer: Displayer[P]) = use((p: P) => displayer.display(p))
//}
//
//object GetValues extends Poly {
//
//  type ValuesGetter[Params <: HList, ParamTypesL <: HList] =
//    LeftFolder.Aux[Params, (HNil.type, Map[String, Any]), this.type, (ParamTypesL, Map[String, Any])]
//
//  implicit def forParameter[Disps <: HList, P <: Parameter[_]](implicit displayer: Displayer[P],
//                                                               prepend: Prepend[Disps, TagMod :: HNil]) =
//    use((acc: (Disps, Map[String, Any]), p: P) => (acc._1 :+ displayer(p, acc._2), acc._2))
//
//}

trait ValueGetter[Params <: HList, ParamTypes] {
  def apply(params: Params, map: Map[String, Any]): ParamTypes
}
object ValueGetter {
  implicit val noParameter = new ValueGetter[HNil, Unit] {
    override def apply(params: HNil, map: Map[String, Any]): Unit = ()
  }

  implicit def oneParameter[ParamType] = new ValueGetter[DisplayableParameter :: HNil, ParamType] {
    override def apply(params: DisplayableParameter :: HNil, map: Map[String, Any]): ParamType =
      map
        .getOrElse(params.head.name, throw new IllegalArgumentException(s"Can't get param ${params.head.name}"))
        .asInstanceOf[ParamType]
  }

  implicit def multiParameters[Params <: HList, ParamType] = new ValueGetter[Params, ParamType] {
    override def apply(params: Params, map: Map[String, Any]): ParamType = ???
  }
}

case class SingleTaskBoard[Params <: HList, /*Disps <: HList,*/ ParamTypes](
  name: String,
  task: Task[Params, ParamTypes, _]
)(
  implicit valueGetter: ValueGetter[Params, ParamTypes]
//  implicit mapper: Mapper.Aux[GetDisplays.type, Params, Disps],
//  toSeq: ToTraversable.Aux[Disps, Seq, TagOf[_]]
) extends Board {

  def fromMap(map: Map[String, Any]): ParamTypes = valueGetter(task.params, map)

  val route = RouterConfigDsl[Board].buildRule { dsl =>
    import dsl._
//    val params = toSeq(task.params.map(GetDisplays))
    staticRoute(name, this) ~> render(SingleTaskBoard.component(SingleTaskBoard.Props(name, Seq.empty)))
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
            <.button(^.onClick --> runTask, "Run"): _*
        )
      }
      .build

  def runTask = Callback(AutowireClient[Api].runTask('ahah).call())
}
