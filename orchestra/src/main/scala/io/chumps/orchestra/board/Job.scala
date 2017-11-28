package io.chumps.orchestra.board

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.java8.time._
import io.k8s.api.core.v1.PodSpec

import io.chumps.orchestra.utils.BaseEncoders._
import org.scalajs.dom.ext.Ajax
import shapeless.ops.function.FnToProduct
import shapeless.{::, _}

import io.chumps.orchestra.job.SimpleJob
import io.chumps.orchestra.model._
import io.chumps.orchestra.parameter.{Parameter, ParameterOperations}
import io.chumps.orchestra.utils.RunIdOperation
import io.chumps.orchestra.{ARunStatus, AStageStatus, AutowireServer, Jobs}

trait Job[ParamValues <: HList, Result, Func, PodSpecFunc] extends Board {

  val id: Symbol
  val name: String

  private[orchestra] trait Api {
    def trigger(runId: RunId, params: ParamValues, tags: Seq[String] = Seq.empty, by: Option[RunInfo] = None): Unit
    def stop(runId: RunId): Unit
    def tags(): Seq[String]
    def history(
      page: Page[Instant]
    ): Seq[(RunId, Instant, ParamValues, Seq[String], ARunStatus[Result], Seq[AStageStatus])]
  }

  private[orchestra] object Api {
    def router(apiServer: Api)(implicit ec: ExecutionContext,
                               encoderP: Encoder[ParamValues],
                               decoderP: Decoder[ParamValues],
                               encoderR: Encoder[Result],
                               decoderR: Decoder[Result]) =
      AutowireServer.route[Api](apiServer)

    val client = new autowire.Client[String, Decoder, Encoder] {
      import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

      override def doCall(req: Request): Future[String] =
        Ajax
          .post(
            url = (Jobs.apiSegment +: Jobs.jobSegment +: id.name +: req.path).mkString("/"),
            data = req.args.asJson.noSpaces,
            responseType = "application/json",
            headers = Map("Content-Type" -> "application/json")
          )
          .map(_.responseText)

      override def read[T: Decoder](raw: String) = decode[T](raw).fold(throw _, identity)
      override def write[T: Encoder](obj: T) = obj.asJson.noSpaces
    }.apply[Api]
  }
}

object Job {

  def apply[Func](id: Symbol, name: String) = new JobBuilder[Func](id, name)

  class JobBuilder[Func](id: Symbol, name: String) {
    // No Params
    def apply[ParamValuesNoRunId <: HList, ParamValues <: HList, Result, PodSpecFunc]()(
      implicit fnToProdFunc: FnToProduct.Aux[Func, ParamValues => Result],
      fnToProdPodSpec: FnToProduct.Aux[PodSpecFunc, ParamValues => PodSpec],
      paramOperations: ParameterOperations[HNil, ParamValuesNoRunId],
      runIdOperation: RunIdOperation[ParamValuesNoRunId, ParamValues],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = SimpleJob[ParamValuesNoRunId, ParamValues, HNil, Result, Func, PodSpecFunc](id, name, HNil)

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
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = SimpleJob[ParamValuesNoRunId, ParamValues, Param :: HNil, Result, Func, PodSpecFunc](id, name, param :: HNil)

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
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      SimpleJob[ParamValuesNoRunId, ParamValues, Params, Result, Func, PodSpecFunc](id, name, tupleToHList.to(params))
  }
}
