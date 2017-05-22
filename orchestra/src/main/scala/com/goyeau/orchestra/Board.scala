package com.goyeau.orchestra

import scala.concurrent.ExecutionContext

import com.goyeau.orchestra.pages.FolderBoardPage
import com.goyeau.orchestra.routes.WebRouter.{AppPage, BoardPage, TaskLogsPage}
import com.goyeau.orchestra.pages.{LogsPage, SingleTaskBoardPage}
import io.circe.Encoder
import io.circe.shapes._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.{::, Generic, HList, HNil, Poly}
import shapeless.ops.hlist.{Comapped, Mapper}

sealed trait Board {
  def pathName: String
  def name: String
  def route(implicit ec: ExecutionContext): StaticDsl.Rule[AppPage]
}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {
  val pathName = name.toLowerCase

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    staticRoute(pathName, BoardPage(this)) ~>
      renderR(ctrl => FolderBoardPage.component(FolderBoardPage.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(pathName)
  }
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}

case class SingleTaskBoard[Func, ParamValues <: HList: Encoder, Params <: HList](
  name: String,
  task: Task.Definition[Func, ParamValues],
  params: Params
)(implicit paramGetter: ParamGetter[Params, ParamValues])
    extends Board {
  val pathName = name.toLowerCase.replaceAll(" ", "")

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    (
      staticRoute(root, BoardPage(this)) ~> renderR { ctrl =>
        SingleTaskBoardPage.component(name, task, params, ctrl)
      } |
        dynamicRouteCT(uuid.caseClass[TaskLogsPage] / "logs") ~> dynRender { page =>
          LogsPage.component(LogsPage.Props(page, task))
        }
    ).prefixPath_/(pathName)
  }
}

object SingleTaskBoard {

  def apply[Func](
    name: String,
    task: Task.Definition[Func, HNil]
  ): SingleTaskBoard[Func, HNil, HNil] =
    SingleTaskBoard[Func, HNil, HNil](name, task, HNil)

  def apply[Func, ParamValue: Encoder](
    name: String,
    key: Task.Definition[Func, ParamValue :: HNil]
  )(param: Parameter[ParamValue]): SingleTaskBoard[Func, ParamValue :: HNil, Parameter[ParamValue] :: HNil] =
    SingleTaskBoard(name, key, param :: HNil)

  def apply[Func, TupledParams, Params <: HList, UniParams <: HList, ParamValues <: HList: Encoder](
    name: String,
    key: Task.Definition[Func, ParamValues]
  )(
    params: TupledParams
  )(
    implicit tupleToHList: Generic.Aux[TupledParams, Params],
    unifyer: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    paramValuesExtracter: Comapped.Aux[UniParams, Parameter, ParamValues],
    paramGetter: ParamGetter[Params, ParamValues]
  ): SingleTaskBoard[Func, ParamValues, Params] =
    SingleTaskBoard(name, key, tupleToHList.to(params))

  private object UnifyParameter extends Poly {
    implicit def forParameter[Param, T](implicit ev: Param <:< Parameter[T]) = use((x: Param) => ev(x))
  }
}
