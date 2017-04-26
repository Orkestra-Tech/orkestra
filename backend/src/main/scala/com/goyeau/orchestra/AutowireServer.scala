package com.goyeau.orchestra

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe
import io.circe.parser._
import io.circe.syntax._

object AutowireServer extends autowire.Server[String, circe.Decoder, circe.Encoder] {
  override def read[T: circe.Decoder](json: String): T =
    decode(json) match {
      case Left(e) => throw new Exception(e)
      case Right(result) => result
    }

  override def write[T: circe.Encoder](obj: T): String = obj.asJson.noSpaces

  def dispatch(url: List[String])(implicit ec: ExecutionContext): RequestContext => Future[RouteResult] =
    entity(as[String]) { entity =>
      val service: Api = new Api {
        override def runTask(task: String): Unit = println("Called runTask")
      }
      val body = read[Map[String, String]](entity)
      val a = autowire.Core.Request(url, body)
      val request = AutowireServer.route[Api](service)(a)
      onSuccess(request)(complete(_))
    }
}
