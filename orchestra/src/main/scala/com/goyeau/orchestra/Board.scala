package com.goyeau.orchestra

import scala.concurrent.ExecutionContext

import com.goyeau.orchestra.page.FolderBoardPage
import com.goyeau.orchestra.route.WebRouter.{AppPage, BoardPage, TaskLogsPage}
import com.goyeau.orchestra.page.{JobBoardPage, LogsPage}
import com.goyeau.orchestra.parameter.{Parameter, ParameterOperations}
import io.circe.Encoder
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.{::, Generic, HList, HNil, Poly}
import shapeless.ops.hlist.{Comapped, Mapper}

sealed trait Board {
  lazy val pathName: String = name.toLowerCase.replaceAll("\\s", "")
  def name: String
  def route(implicit ec: ExecutionContext): StaticDsl.Rule[AppPage]
}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {

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

case class JobBoard[ParamValues <: HList: Encoder, Params <: HList](
  name: String,
  job: Job.Definition[_, ParamValues, _],
  params: Params
)(implicit paramGetter: ParameterOperations[Params, ParamValues])
    extends Board {

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    (
      staticRoute(root, BoardPage(this)) ~> renderR { ctrl =>
        JobBoardPage.component(JobBoardPage.Props(name, job, params, ctrl))
      } |
        dynamicRoute(uuid.xmap(TaskLogsPage(job, _))(_.runId) / "logs") { case p @ TaskLogsPage(`job`, _) => p } ~>
          dynRender { page =>
            LogsPage.component(LogsPage.Props(page))
          }
    ).prefixPath_/(pathName)
  }
}

object JobBoard {

  def apply(
    name: String,
    job: Job.Definition[_, HNil, _]
  ): JobBoard[HNil, HNil] =
    JobBoard[HNil, HNil](name, job, HNil)

  def apply[Param <: Parameter[ParamValue], ParamValue: Encoder](
    name: String,
    job: Job.Definition[_, ParamValue :: HNil, _]
  )(
    param: Param
  )(
    implicit paramGetter: ParameterOperations[Param :: HNil, ParamValue :: HNil]
  ): JobBoard[ParamValue :: HNil, Param :: HNil] =
    JobBoard(name, job, param :: HNil)

  def apply[TupledParams, Params <: HList, UniParams <: HList, ParamValues <: HList: Encoder](
    name: String,
    job: Job.Definition[_, ParamValues, _]
  )(
    params: TupledParams
  )(
    implicit tupleToHList: Generic.Aux[TupledParams, Params],
    unifier: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    paramValuesExtractor: Comapped.Aux[UniParams, Parameter, ParamValues],
    paramGetter: ParameterOperations[Params, ParamValues]
  ): JobBoard[ParamValues, Params] =
    JobBoard(name, job, tupleToHList.to(params))

  private object UnifyParameter extends Poly {
    implicit def forParameter[Param, T](implicit ev: Param <:< Parameter[T]) = use((x: Param) => ev(x))
  }
}
