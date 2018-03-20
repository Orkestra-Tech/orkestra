package com.drivetribe.orchestra.utils

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax

import com.drivetribe.orchestra.OrchestraConfig

object AutowireClient {

  def apply(segment: String) = new autowire.Client[Json, Decoder, Encoder] {
    override def doCall(request: Request): Future[Json] =
      Ajax
        .post(
          url = (OrchestraConfig.apiSegment +: segment +: request.path).mkString("/"),
          data = request.args.asJson.noSpaces,
          responseType = "application/json",
          headers = Map("Content-Type" -> "application/json")
        )
        .map(request => parse(request.responseText).fold(throw _, identity))

    override def read[T: Decoder](json: Json) = json.as[T].fold(throw _, identity)
    override def write[T: Encoder](obj: T) = obj.asJson
  }
}
