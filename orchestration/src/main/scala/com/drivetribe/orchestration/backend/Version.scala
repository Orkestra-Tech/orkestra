package com.drivetribe.orchestration.backend

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.util.ByteString
import com.drivetribe.orchestration.infrastructure.Environment
import com.goyeau.orchestra.AkkaImplicits._

object Version {

  def apply(environment: Environment): String = {
    val result = for {
      response <- Http().singleRequest(HttpRequest(uri = s"${environment.monitoringApi}/versions"))
      entity <- response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } yield entity.utf8String

    Await.result(result, Duration.Inf)
  }
}
