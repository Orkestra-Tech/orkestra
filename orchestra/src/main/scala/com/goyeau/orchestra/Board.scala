package com.goyeau.orchestra

import scala.concurrent.ExecutionContext

import com.goyeau.orchestra.pages.FolderBoardPage
import com.goyeau.orchestra.routes.WebRouter.{AppPage, BoardPage, TaskLogsPage}
import com.goyeau.orchestra.pages.{LogsPage, SingleTaskBoardPage}
import io.circe.Encoder
import io.circe.shapes._
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

case class SingleJobBoard[Func, ParamValues <: HList: Encoder, Params <: HList](
  name: String,
  job: Job.Definition[Func, ParamValues],
  params: Params
)(implicit paramGetter: ParamGetter[Params, ParamValues])
    extends Board {
  val pathName = name.toLowerCase.replaceAll(" ", "")

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    (
      staticRoute(root, BoardPage(this)) ~> renderR { ctrl =>
        SingleTaskBoardPage.component(name, job, params, ctrl)
      } |
        dynamicRoute(uuid.xmap(TaskLogsPage(job, _))(_.runId) / "logs") { case p @ TaskLogsPage(`job`, _) => p } ~>
          dynRender { page =>
            LogsPage.component(LogsPage.Props(page))
          }
    ).prefixPath_/(pathName)
  }
}

object SingleJobBoard {

  def apply[Func](
    name: String,
    job: Job.Definition[Func, HNil]
  ): SingleJobBoard[Func, HNil, HNil] =
    SingleJobBoard[Func, HNil, HNil](name, job, HNil)

  def apply[Func, Param <: Parameter[ParamValue], ParamValue: Encoder](
    name: String,
    job: Job.Definition[Func, ParamValue :: HNil]
  )(
    param: Param
  )(
    implicit paramGetter: ParamGetter[Param :: HNil, ParamValue :: HNil]
  ): SingleJobBoard[Func, ParamValue :: HNil, Param :: HNil] =
    SingleJobBoard(name, job, param :: HNil)

  def apply[Func, TupledParams, Params <: HList, UniParams <: HList, ParamValues <: HList: Encoder](
    name: String,
    job: Job.Definition[Func, ParamValues]
  )(
    params: TupledParams
  )(
    implicit tupleToHList: Generic.Aux[TupledParams, Params],
    unifier: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    paramValuesExtractor: Comapped.Aux[UniParams, Parameter, ParamValues],
    paramGetter: ParamGetter[Params, ParamValues]
  ): SingleJobBoard[Func, ParamValues, Params] =
    SingleJobBoard(name, job, tupleToHList.to(params))

  private object UnifyParameter extends Poly {
    implicit def forParameter[Param, T](implicit ev: Param <:< Parameter[T]) = use((x: Param) => ev(x))
  }
}
