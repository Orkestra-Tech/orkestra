package com.drivetribe.orchestration.frontend

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import com.drivetribe.orchestration.infrastructure.Environment
import com.goyeau.orchestra.AkkaImplicits._

object Version {
  def apply(environment: Environment): String = {
    val versionHeader = "X-Drivetribe-Version"
    val result = Http()
      .singleRequest(HttpRequest(uri = environment.frontend.toString))
      .map(
        _.headers
          .find(_.name.equalsIgnoreCase(versionHeader))
          .fold(throw new IllegalStateException(s"$versionHeader header in missing from the frontend response"))(
            _.value
          )
      )

    Await.result(result, Duration.Inf)
  }
}
