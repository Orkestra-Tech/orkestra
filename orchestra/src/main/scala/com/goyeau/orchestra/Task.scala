package com.goyeau.orchestra

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import autowire.Core
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import io.circe
import shapeless._
import shapeless.ops.hlist._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.scalajs.dom.ext.Ajax
import io.circe.syntax._
import io.circe.generic.auto._

case class Task[Params <: HList, ParamValue /*: Decoder*/, Result /*: Encoder*/ ](id: Symbol,
                                                                                  params: Params,
                                                                                  task: ParamValue => Result) {
  trait Api {
    def run(taskInfo: Task.Info, params: ParamValue): Result
  }

  object ApiServer extends Api {
    override def run(taskInfo: Task.Info, params: ParamValue): Result = task(params)

//    def dispatch(url: List[String])(implicit ec: ExecutionContext): RequestContext => Future[RouteResult] =
//      entity(as[Json]) { entity =>
//        val body = AutowireServer.read[Map[String, String]](entity).mapValues(decode[Json](_).fold(throw _, identity))
//        val request = AutowireServer.route[Api](ApiServer)(Core.Request(url, body))
//        onSuccess(request)(complete(_))
//      }
  }

  //  val apiClient = new AutowireClient()[Api]
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

  implicit def noParameter[Result](f: => Result) =
    new ParamMagnet {
      type Out = Task[HNil, Unit, Result]
      def apply(id: Symbol) = Task(id, HNil, (_: Unit) => f)
    }

  implicit def oneParameter[T, Param[T] <: Parameter[T]](param: Param[T]) =
    new ParamMagnet {
      type Out = TaskBuilder[Param[T] :: HNil, T]
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
    tupledParamTypes: Tupler.Aux[ParamTypes, ParamValues]
  ) =
    new ParamMagnet {
      type Out = TaskBuilder[Params, ParamValues]
      def apply(id: Symbol) = TaskBuilder(id, tuple2HList.to(params))
    }

  case class TaskBuilder[Params <: HList, ParamValues](id: Symbol, params: Params) {
    def apply[Result](f: ParamValues => Result) =
      Task[Params, ParamValues, Result](id, params, f)
  }

  object UnifyParameter extends Poly {
    implicit def forParameter[P, T](implicit ev: P <:< Parameter[T]) = use((x: P) => ev(x))
  }
}
