package io.chumps.orchestra.board

import io.circe.Encoder
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless._

import io.chumps.orchestra._
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.page.JobBoardPage
import io.chumps.orchestra.parameter.{Parameter, ParameterOperations}
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}

case class JobBoard[ParamValues <: HList: Encoder, Params <: HList](
  job: Job.Definition[_, ParamValues, _],
  params: Params
)(implicit paramGetter: ParameterOperations[Params, ParamValues])
    extends Board {
  val name = job.name

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    val breadcrumb = parentBreadcrumb :+ pathName
    dynamicRoute(
      pathName ~ ("/" ~ uuid).option.xmap(uuid => BoardPageRoute(breadcrumb, uuid.map(RunId(_))))(_.runId.map(_.value))
    ) {
      case p @ BoardPageRoute(bc, _) if bc == breadcrumb => p
    } ~> dynRenderR((page, ctrl) => JobBoardPage.component(JobBoardPage.Props(job, params, page.runId, ctrl)))
  }
}

object JobBoard {

  def apply(job: Job.Definition[_, HNil, _]): JobBoard[HNil, HNil] =
    JobBoard[HNil, HNil](job, HNil)

  def apply(
    job: Job.Definition[_, RunId :: HNil, _]
  )(implicit dummyImplicit: DummyImplicit): JobBoard[RunId :: HNil, HNil] =
    JobBoard[RunId :: HNil, HNil](job, HNil)

  def apply[Param <: Parameter[ParamValue], ParamValue: Encoder](
    job: Job.Definition[_, ParamValue :: HNil, _]
  )(
    param: Param
  )(
    implicit paramGetter: ParameterOperations[Param :: HNil, ParamValue :: HNil]
  ): JobBoard[ParamValue :: HNil, Param :: HNil] =
    JobBoard(job, param :: HNil)

  def apply[Param <: Parameter[ParamValue], ParamValue: Encoder](
    job: Job.Definition[_, ParamValue :: RunId :: HNil, _]
  )(
    param: Param
  )(
    implicit paramGetter: ParameterOperations[Param :: HNil, ParamValue :: RunId :: HNil],
    dummyImplicit: DummyImplicit
  ): JobBoard[ParamValue :: RunId :: HNil, Param :: HNil] =
    JobBoard(job, param :: HNil)

  def apply[Param <: Parameter[ParamValue], ParamValue: Encoder](
    job: Job.Definition[_, RunId :: ParamValue :: HNil, _]
  )(
    param: Param
  )(
    implicit paramGetter: ParameterOperations[Param :: HNil, RunId :: ParamValue :: HNil],
    dummyImplicit: DummyImplicit,
    dummyImplicit2: DummyImplicit
  ): JobBoard[RunId :: ParamValue :: HNil, Param :: HNil] =
    JobBoard(job, param :: HNil)

  def apply[TupledParams, Params <: HList, ParamValues <: HList: Encoder](
    job: Job.Definition[_, ParamValues, _]
  )(
    params: TupledParams
  )(
    implicit tupleToHList: Generic.Aux[TupledParams, Params],
    paramGetter: ParameterOperations[Params, ParamValues]
  ): JobBoard[ParamValues, Params] =
    JobBoard(job, tupleToHList.to(params))
}
