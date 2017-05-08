package com.goyeau.orchestra

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import autowire.Core
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._

object AutowireServer extends autowire.Server[Json, circe.Decoder, circe.Encoder] {
  override def read[T: circe.Decoder](json: Json): T =
    json.as[T].fold(throw _, identity)

  override def write[T: circe.Encoder](obj: T): Json = obj.asJson

  def dispatch(url: List[String])(implicit ec: ExecutionContext): RequestContext => Future[RouteResult] =
    entity(as[Json]) { entity =>
      val service: Api = new Api {
        override def runTask(task: Symbol) = Future.successful(println("Called runTask with id " + task))
      }
      val body = read[Map[String, String]](entity).mapValues(decode[Json](_).fold(throw _, identity))
      val request = AutowireServer.route[Api](service)(Core.Request(url, body))
      onSuccess(request)(complete(_))
    }
}
