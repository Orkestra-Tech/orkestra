package io.chumps.orchestra.board

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.utils.BaseEncoders._
import io.k8s.api.core.v1.PodSpec
import org.scalajs.dom.ext.Ajax
import shapeless.ops.function.FnToProduct
import shapeless.{::, _}

import io.chumps.orchestra.job.{JobRunner, SimpleJob}
import io.chumps.orchestra.model._
import io.chumps.orchestra.parameter.{Parameter, ParameterOperations}
import io.chumps.orchestra.{ARunStatus, AStageStatus, AutowireServer, Jobs}

trait Job[Func, ParamValues <: HList] extends Board {

  val id: Symbol
  val name: String

  override lazy val pathName = id.name.toLowerCase

  def apply[Result](job: Func)(implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
                               encoderP: Encoder[ParamValues],
                               decoderP: Decoder[ParamValues],
                               encoderR: Encoder[Result],
                               decoderR: Decoder[Result]) =
    JobRunner(this, PodSpec(Seq.empty), fnToProd(job))

  def apply[Result](podConfig: PodSpec)(job: Func)(implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
                                                   encoderP: Encoder[ParamValues],
                                                   decoderP: Decoder[ParamValues],
                                                   encoderR: Encoder[Result],
                                                   decoderR: Decoder[Result]) =
    JobRunner[ParamValues, Result](this, podConfig, fnToProd(job))

  private[orchestra] trait Api {
    def trigger(runId: RunId, params: ParamValues, tags: Seq[String] = Seq.empty): ARunStatus
    def stop(runId: RunId): Unit
    def tags(): Seq[String]
    def history(page: Page[Instant]): Seq[(RunId, Instant, ParamValues, Seq[String], ARunStatus, Seq[AStageStatus])]
  }

  private[orchestra] object Api {
    def router(
      apiServer: Api
    )(implicit ec: ExecutionContext, encoder: Encoder[ParamValues], decoder: Decoder[ParamValues]) =
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
    def apply[ParamValues <: HList, Result]()(
      implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
      paramOperations: ParameterOperations[HNil, ParamValues],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = SimpleJob[Func, ParamValues, HNil](id, name, HNil)

    // One param
    def apply[ParamValues <: HList, Param <: Parameter[_], Result](param: Param)(
      implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
      paramOperations: ParameterOperations[Param :: HNil, ParamValues],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = SimpleJob[Func, ParamValues, Param :: HNil](id, name, param :: HNil)

    // Multi params
    def apply[ParamValues <: HList, TupledParams, Params <: HList, Result](params: TupledParams)(
      implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
      tupleToHList: Generic.Aux[TupledParams, Params],
      paramOperations: ParameterOperations[Params, ParamValues],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = SimpleJob[Func, ParamValues, Params](id, name, tupleToHList.to(params))
  }

  implicit def encoder[Func, ParamValues <: HList]: Encoder[Job[Func, ParamValues]] =
    (o: Job[Func, ParamValues]) =>
      Json.obj(
        "id" -> o.id.asJson,
        "name" -> Json.fromString(o.name)
    )

  implicit def decoder[Func, ParamValues <: HList: Encoder: Decoder]: Decoder[Job[Func, ParamValues]] =
    (c: HCursor) =>
      for {
        jobId <- c.downField("id").as[Symbol]
        jobName <- c.downField("name").as[String]
      } yield
        new Job[Func, ParamValues] {
          override val id = jobId
          override val name = jobName
          override def route(parentBreadcrumb: Seq[String]) = ???
      }

  implicit val decoderDefault: Decoder[Job[_, _ <: HList]] = (c: HCursor) =>
    for {
      jobId <- c.downField("id").as[Symbol]
      jobName <- c.downField("name").as[String]
    } yield
      new Job[Nothing, HNil] {
        override val id = jobId
        override val name = jobName
        override def route(parentBreadcrumb: Seq[String]) = ???
    }
}
