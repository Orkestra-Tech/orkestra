package com.goyeau.orchestra

import scala.language.{higherKinds, implicitConversions}

import shapeless._
import shapeless.ops.hlist._
import shapeless.ops.function._

trait Task[Params <: HList, Func] {
  def params: Params
  def f: Func
}

case class ParameterisedTask[Params <: HList, Func](params: Params, f: Func) extends Task[Params, Func]

object Task {
  def apply(magnet: ParamMagnet): magnet.Out = magnet()

  object FetchParams extends Poly {
    implicit def forParameter[Result <: HList, T](implicit prepend: Prepend[Result, T :: HNil], decode: Encoder[T]) =
      use((c: (Result, Map[String, String]), p: Parameter[T]) => (c._1 :+ decode(c._2(p.name)), c._2))
  }

  def run[Params <: HList, Func, ParamTypes <: HList](job: Task[Params, Func], givenParams: Map[String, String])(
    implicit folder: LeftFolder.Aux[Params,
                                    (HNil.type, Map[String, String]),
                                    FetchParams.type,
                                    (ParamTypes, Map[String, String])],
    fn2Prod: FnToProduct.Aux[Func, ParamTypes => Unit]
  ): Unit = {
    val (fetchedParams, _) = job.params.foldLeft((HNil, givenParams))(FetchParams)
    fn2Prod(job.f)(fetchedParams)
  }
}

// Trick to hide shapeless implicits
sealed trait ParamMagnet {
  type Out
  def apply(): Out
}

object ParamMagnet {

  implicit def noParameter(f: => Unit) =
    new ParamMagnet {
      type Out = Task[Parameter[String] :: HNil, String => Unit]
      def apply() = ParameterisedTask((RunId: Parameter[String]) :: HNil, (_: String) => f)
    }

  implicit def oneParameter[Param, T](param: Parameter[T]) =
    new ParamMagnet {
      type Out = (Param => Unit) => Task[Parameter[T] :: HNil, Param => Unit]
      def apply() = (f: Param => Unit) => ParameterisedTask(param :: HNil, f)
    }

  implicit def multiParameters[TupledParams, RawParams <: HList, Params <: HList, ParamTypes <: HList, Func](
    params: TupledParams
  )(implicit tuple2HList: Generic.Aux[TupledParams, RawParams],
    mapper: Mapper.Aux[UnifyParameter.type, RawParams, Params],
    comapper: Comapped.Aux[Params, Parameter, ParamTypes],
    fun2Prod: FnToProduct.Aux[Func, ParamTypes => Unit]) =
    new ParamMagnet {
      type Out = Func => Task[RawParams, Func]
      def apply() = (f: Func) => ParameterisedTask(tuple2HList.to(params), f)
    }

  object UnifyParameter extends Poly {
    implicit def forParameter[P, T](implicit ev: P <:< Parameter[T]) = use((x: P) => ev(x))
  }
}
