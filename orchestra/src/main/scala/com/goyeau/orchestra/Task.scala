package com.goyeau.orchestra

import scala.concurrent.ExecutionContext
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import shapeless._
import shapeless.ops.hlist._

case class Task[Params <: HList, ParamValue: Decoder, Result: Encoder](id: Symbol,
                                                                       params: Params,
                                                                       task: ParamValue => Result) {
  trait Api {
    def run(taskInfo: Task.Info, params: ParamValue): Boolean // TODO: Change Boolean for a Status enum, Running, Failure[Exception], Success[Result]...
  }

  object ApiServer extends Api {
    override def run(taskInfo: Task.Info, params: ParamValue): Boolean = {
      task(params)
      true
    }

    def route(url: List[String])(implicit ec: ExecutionContext): Route =
      entity(as[String]) { entity =>
        val body = AutowireServer.read[Map[String, String]](entity)
        val request = AutowireServer.route[Api](ApiServer)(Core.Request(url, body))
        onSuccess(request)(complete(_))
      }
  }

  val apiClient = AutowireClient(id)[Api]
}

object Task {
  def apply(id: Symbol)(magnet: ParamMagnet): magnet.Out = magnet(id)

  case class Info(runId: String)
}

// Trick to hide shapeless implicits
sealed trait ParamMagnet {
  type Out
  def apply(id: Symbol): Out
}

object ParamMagnet {

  implicit def noParameter[Result: Encoder](f: => Result) =
    new ParamMagnet {
      type Out = Task[HNil, Unit, Result]
      def apply(id: Symbol) = Task(id, HNil, (_: Unit) => f)
    }

  implicit def oneParameter[Param[T] <: Parameter[T], ParamValue: Decoder](param: Param[ParamValue]) = // TODO: Ask Aldo why
    new ParamMagnet {
      type Out = TaskBuilder[Param[ParamValue] :: HNil, ParamValue]
      def apply(id: Symbol) = TaskBuilder(id, param :: HNil)
    }

  implicit def multiParameters[TupledParams,
                               Params <: HList,
                               UniParams <: HList,
                               ParamTypes <: HList,
                               ParamValues <: Product](
    params: TupledParams
  )(
    implicit tuple2HList: Generic.Aux[TupledParams, Params],
    unifyer: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    comapper: Comapped.Aux[UniParams, Parameter, ParamTypes],
    tupledParamTypes: Tupler.Aux[ParamTypes, ParamValues],
    decoder: Decoder[ParamValues]
  ) =
    new ParamMagnet {
      type Out = TaskBuilder[Params, ParamValues]
      def apply(id: Symbol) = TaskBuilder(id, tuple2HList.to(params))
    }

  case class TaskBuilder[Params <: HList, ParamValues: Decoder](id: Symbol, params: Params) {
    def apply[Result: Encoder](f: ParamValues => Result) =
      Task[Params, ParamValues, Result](id, params, f)
  }

  object UnifyParameter extends Poly {
    implicit def forParameter[Param, T](implicit ev: Param <:< Parameter[T]) = use((x: Param) => ev(x))
  }
}
