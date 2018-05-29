package com.goyeau.orchestra.board

import com.goyeau.orchestra.model.{JobId, RunId}
import com.goyeau.orchestra.page.JobPage
import com.goyeau.orchestra.parameter.ParameterOperations
import com.goyeau.orchestra.route.LogsRoute
import com.goyeau.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}
import com.goyeau.orchestra.utils.RunIdOperation
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless._

case class SimpleJobBoard[
  ParamValuesNoRunId <: HList,
  ParamValues <: HList: Encoder: Decoder,
  Params <: HList,
  Result: Decoder,
  Func,
  PodSpecFunc
](id: JobId, name: String, params: Params)(
  implicit paramOperations: ParameterOperations[Params, ParamValuesNoRunId],
  runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues]
) extends JobBoard[ParamValues, Result, Func, PodSpecFunc] {

  def route(parentBreadcrumb: Seq[String]) = RouterConfigDsl[PageRoute].buildRule { dsl =>
    import dsl._
    val job = dynamicRoute(
      ("?runId=" ~ uuid).option
        .xmap(uuid => BoardPageRoute(parentBreadcrumb, this, uuid.map(RunId(_))))(_.runId.map(_.value))
    ) {
      case p @ BoardPageRoute(`parentBreadcrumb`, b, _) if b == this => p
    } ~> dynRenderR((page, ctrl) => JobPage.component(JobPage.Props(this, params, page, ctrl)))

    (job | LogsRoute(parentBreadcrumb :+ name)).prefixPath_/(id.value.toLowerCase)
  }
}
