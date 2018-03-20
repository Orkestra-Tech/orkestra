package com.drivetribe.orchestra.job

import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react.extra.router.RouterConfigDsl
import japgolly.scalajs.react.vdom.html_<^._
import shapeless._

import com.drivetribe.orchestra.board.Job
import com.drivetribe.orchestra.model.{JobId, RunId}
import com.drivetribe.orchestra.page.JobPage
import com.drivetribe.orchestra.parameter.ParameterOperations
import com.drivetribe.orchestra.route.LogsRoute
import com.drivetribe.orchestra.route.WebRouter.{BoardPageRoute, PageRoute}
import com.drivetribe.orchestra.utils.RunIdOperation

case class SimpleJob[ParamValuesNoRunId <: HList,
                     ParamValues <: HList: Encoder: Decoder,
                     Params <: HList,
                     Result: Decoder,
                     Func,
                     PodSpecFunc](id: JobId, name: String, params: Params)(
  implicit paramOperations: ParameterOperations[Params, ParamValuesNoRunId],
  runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues]
) extends Job[ParamValues, Result, Func, PodSpecFunc] {

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
