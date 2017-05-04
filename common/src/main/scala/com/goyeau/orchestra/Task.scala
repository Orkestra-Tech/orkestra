package com.goyeau.orchestra

import scala.language.{higherKinds, implicitConversions}

import shapeless._
import shapeless.ops.hlist._

case class Task[Params <: HList, ParamValue, Result](id: Symbol, params: Params, task: ParamValue => Result) {
  trait Api {
    def run(runId: String, params: ParamValue): Result
  }
  object ApiImpl extends Api {
    override def run(runId: String, params: ParamValue): Result = task(params)
  }
}

object Task {
  def apply(id: Symbol)(magnet: ParamMagnet): magnet.Out = magnet(id)

//  object FetchParams extends Poly {
//    implicit def forParameter[Result <: HList, T](implicit prepend: Prepend[Result, T :: HNil], decode: Encoder[T]) =
//      use((c: (Result, Map[String, String]), p: Parameter[T]) => (c._1 :+ decode(c._2(p.name)), c._2))
//  }
//
//  def run[Params <: HList, Func, ParamTypes <: HList](job: Task[Params, Func], givenParams: Map[String, String])(
//    implicit folder: LeftFolder.Aux[Params,
//                                    (HNil.type, Map[String, String]),
//                                    FetchParams.type,
//                                    (ParamTypes, Map[String, String])],
//    fn2Prod: FnToProduct.Aux[Func, ParamTypes => Unit]
//  ): Unit = {
//    val (fetchedParams, _) = job.params.foldLeft((HNil, givenParams))(FetchParams)
//    fn2Prod(job.task)(fetchedParams)
//  }
}

// Trick to hide shapeless implicits
sealed trait ParamMagnet {
  type Out
  def apply(id: Symbol): Out
}

object ParamMagnet {

//  def f(p: Unit, f: () => String) = f(p)

  implicit def noParameter[Result](f: => Result) =
    new ParamMagnet {
      type Out = Task[HNil, Unit, Result]
      def apply(id: Symbol) = Task(id, HNil, (_: Unit) => f)
    }

  implicit def oneParameter[Param, T](param: Parameter[T]) =
    new ParamMagnet {
      type Out = TaskBuilder[Parameter[T] :: HNil, T]
      def apply(id: Symbol) = TaskBuilder(id, param :: HNil)
    }

  implicit def multiParameters[TupledParams,
                               Params <: HList,
                               UniParams <: HList,
                               ParamTypes <: HList,
                               TupledParamTypes <: Product](
    params: TupledParams
  )(
    implicit tuple2HList: Generic.Aux[TupledParams, Params],
    unifyer: Mapper.Aux[UnifyParameter.type, Params, UniParams],
    comapper: Comapped.Aux[UniParams, Parameter, ParamTypes],
    tupledParamTypes: Tupler.Aux[ParamTypes, TupledParamTypes]
  ) =
    new ParamMagnet {
      type Out = TaskBuilder[Params, TupledParamTypes]
      def apply(id: Symbol) = TaskBuilder(id, tuple2HList.to(params))
    }

  case class TaskBuilder[Params <: HList, ParamTypes](id: Symbol, params: Params) {
    def apply[Result, Func](f: ParamTypes => Result) =
      Task[Params, ParamTypes, Result](id, params, f)
  }
}

object UnifyParameter extends Poly {
  implicit def forParameter[P, T](implicit ev: P <:< Parameter[T]) = use((x: P) => ev(x))
}
