package com.drivetribe.orchestration.backend

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.util.ByteString
import com.drivetribe.orchestration.Environment
import com.goyeau.orchestra.AkkaImplicits._
import io.circe.parser._
import io.circe.generic.auto._

case class Version(build: String, commitId: String, state: String, colour: String)

object Version {

  def apply(environment: Environment): Version = {
    val version = for {
      response <- Http().singleRequest(HttpRequest(uri = s"${environment.monitoringApi}/versions"))
      entity <- response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    } yield decode[Version](entity.utf8String).fold(throw _, identity)

    Await.result(version, Duration.Inf)
  }
}
