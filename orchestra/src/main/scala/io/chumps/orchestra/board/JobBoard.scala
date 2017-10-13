package io.chumps.orchestra.board

import io.circe.Encoder
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless._
import shapeless.ops.hlist.{Comapped, Mapper}

import io.chumps.orchestra._
import io.chumps.orchestra.page.JobBoardPage
import io.chumps.orchestra.parameter.{Parameter, ParameterOperations}
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class JobBoard[ParamValues <: HList: Encoder, Params <: HList](
  job: Job.Definition[_, ParamValues, _],
  params: Params
)(implicit paramGetter: ParameterOperations[Params, ParamValues])
    extends Board {
  val name = job.name

  val route = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    (staticRoute(root, BoardPageRoute(this)) ~> renderR { ctrl =>
      JobBoardPage.component(JobBoardPage.Props(job, params, ctrl))
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
