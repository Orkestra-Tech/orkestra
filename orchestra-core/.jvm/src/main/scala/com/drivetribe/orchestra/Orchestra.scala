package com.drivetribe.orchestra

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{entity, _}
import autowire.Core
import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.Logger
import io.circe.Json
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.shapes._
import io.circe.java8.time._

import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.kubernetes.Kubernetes
import com.drivetribe.orchestra.route.BackendRoutes
import com.drivetribe.orchestra.utils.{AutowireServer, Elasticsearch}

trait Orchestra extends BackendRoutes with OrchestraPlugin {
  private lazy val logger = Logger(getClass)
  override implicit lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override implicit lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override implicit lazy val elasticsearchClient: HttpClient = Elasticsearch.client

  def jobRunners: Set[JobRunner[_, _]]

  override lazy val routes = super.routes ~
    pathPrefix(OrchestraConfig.apiSegment) {
      pathPrefix(OrchestraConfig.jobSegment) {
        jobRunners.map(_.apiRoute).reduce(_ ~ _)
      } ~
        path(OrchestraConfig.commonSegment / Segments) { segments =>
          entity(as[String]) { entity =>
            val body = AutowireServer.read[Map[String, Json]](parse(entity).fold(throw _, identity))
            val request = AutowireServer.route[CommonApi](CommonApiServer())(Core.Request(segments, body))
            onSuccess(request)(json => complete(json.noSpaces))
          }
        }
    }

  def main(args: Array[String]): Unit =
    Await.result(
      orchestraConfig.runInfoMaybe.fold {
        for {
          _ <- Future(logger.info("Initializing Elasticsearch"))
          _ <- Elasticsearch.init()
          _ = logger.info("Starting master Orchestra")
          _ <- onMasterStart()
          _ <- Http().bindAndHandle(routes, "0.0.0.0", orchestraConfig.port)
        } yield ()
      } { runInfo =>
        for {
          _ <- Future(logger.info(s"Running job $runInfo"))
          _ <- onJobStart(runInfo)
          _ <- jobRunners
            .find(_.job.id == runInfo.jobId)
            .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.jobId}"))
            .start(runInfo)
        } yield ()
      },
      Duration.Inf
    )
}
