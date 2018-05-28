package com.drivetribe.orchestra.board

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.model._
import com.drivetribe.orchestra.parameter.{Parameter, ParameterOperations}
import com.drivetribe.orchestra.utils.{AutowireClient, AutowireServer, RunIdOperation}
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.java8.time._
import io.k8s.api.core.v1.PodSpec
import shapeless.ops.function.FnToProduct
import shapeless.{::, _}

trait JobBoard[ParamValues <: HList, Result, Func, PodSpecFunc] extends Board {
  val id: JobId
  val segment = id.value
  val name: String

  private[orchestra] trait Api {
    def trigger(
      runId: RunId,
      params: ParamValues,
      tags: Seq[String] = Seq.empty,
      by: Option[RunInfo] = None
    ): Future[Unit]
    def stop(runId: RunId): Future[Unit]
    def tags(): Future[Seq[String]]
    def history(page: Page[Instant]): Future[History[ParamValues, Result]]
  }

  private[orchestra] object Api {
    def router(apiServer: Api)(
      implicit ec: ExecutionContext,
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      AutowireServer.route[Api](apiServer)

    val client = AutowireClient(s"${OrchestraConfig.jobSegment}/${id.value}")[Api]
  }
}

object JobBoard {

  /**
    * Create a JobBoard.
    * A JobBoard defines the type of the function it runs, a unique id and a pretty name for the UI.
    *
    * @param id A unique JobId
    * @param name A pretty name for the display
    */
  def apply[Func](id: JobId, name: String) = new JobBuilder[Func](id, name)

  class JobBuilder[Func](id: JobId, name: String) {
    // No Params
    def apply[ParamValuesNoRunId <: HList, ParamValues <: HList, Result, PodSpecFunc]()(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      paramOperations: ParameterOperations[HNil, ParamValuesNoRunId],
      runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      decoderR: Decoder[Result]
    ) = SimpleJobBoard[ParamValuesNoRunId, ParamValues, HNil, Result, Func, PodSpecFunc](id, name, HNil)

    // One param
    def apply[ParamValuesNoRunId <: HList, ParamValues <: HList, Param <: Parameter[_], Result, PodSpecFunc](
      param: Param
    )(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
      paramOperations: ParameterOperations[Param :: HNil, ParamValuesNoRunId],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      decoderR: Decoder[Result]
    ) =
      SimpleJobBoard[ParamValuesNoRunId, ParamValues, Param :: HNil, Result, Func, PodSpecFunc](id, name, param :: HNil)

    // Multi params
    def apply[ParamValuesNoRunId <: HList, ParamValues <: HList, TupledParams, Params <: HList, Result, PodSpecFunc](
      params: TupledParams
    )(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      tupleToHList: Generic.Aux[TupledParams, Params],
      runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
      paramOperations: ParameterOperations[Params, ParamValuesNoRunId],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      decoderR: Decoder[Result]
    ) =
      SimpleJobBoard[ParamValuesNoRunId, ParamValues, Params, Result, Func, PodSpecFunc](
        id,
        name,
        tupleToHList.to(params)
      )
  }
}
