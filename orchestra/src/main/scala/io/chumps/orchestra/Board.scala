package io.chumps.orchestra

import io.chumps.orchestra.page.FolderBoardPage
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}
import io.chumps.orchestra.page.JobBoardPage
import io.chumps.orchestra.parameter.{Parameter, ParameterOperations}
import io.circe.Encoder
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.{::, Generic, HList, HNil, Poly}
import shapeless.ops.hlist.{Comapped, Mapper}

sealed trait Board {
  lazy val pathName: String = name.toLowerCase.replaceAll("\\s", "")
  def name: String
  def route: StaticDsl.Rule[PageRoute]
}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {

  def route = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    staticRoute(pathName, BoardPageRoute(this)) ~>
      renderR(ctrl => FolderBoardPage.component(FolderBoardPage.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(pathName)
  }
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}

case class JobBoard[ParamValues <: HList: Encoder, Params <: HList](
  job: Job.Definition[_, ParamValues, _],
  params: Params
)(implicit paramGetter: ParameterOperations[Params, ParamValues])
    extends Board {
  val name = job.name

  val route = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    (staticRoute(root, BoardPageRoute(this)) ~> renderR { ctrl =>
      JobBoardPage.component(JobBoardPage.Props(name, job, params, ctrl))
    }).prefixPath_/(pathName)
  }
}

object JobBoard {

  def apply(job: Job.Definition[_, HNil, _]): JobBoard[HNil, HNil] =
    JobBoard[HNil, HNil](job, HNil)

  def apply[Param <: Parameter[ParamValue], ParamValue: Encoder](
    job: Job.Definition[_, ParamValue :: HNil, _]
  )(
    param: Param
  )(
    implicit paramGetter: ParameterOperations[Param :: HNil, ParamValue :: HNil]
  ): JobBoard[ParamValue :: HNil, Param :: HNil] =
    JobBoard(job, param :: HNil)

  def apply[TupledParams, Params <: HList, UniParams <: HList, ParamValues <: HList: Encoder](
    job: Job.Definition[_, ParamValues, _]
  )(
    params: TupledParams
  )(
    implicit tupleToHList: Generic.Aux[TupledParams, Params],
    unifier: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    paramValuesExtractor: Comapped.Aux[UniParams, Parameter, ParamValues],
    paramGetter: ParameterOperations[Params, ParamValues]
  ): JobBoard[ParamValues, Params] =
    JobBoard(job, tupleToHList.to(params))

  private object UnifyParameter extends Poly {
    implicit def forParameter[Param, T](implicit ev: Param <:< Parameter[T]) = use((x: Param) => ev(x))
  }
}
