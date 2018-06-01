package com.goyeau.orkestra

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{entity, _}
import autowire.Core
import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.shapes._
import io.circe.java8.time._
import com.goyeau.orkestra.utils.AkkaImplicits._
import com.goyeau.orkestra.job.Job
import com.goyeau.orkestra.kubernetes.Kubernetes
import com.goyeau.orkestra.utils.{AutowireServer, Elasticsearch}
import scalajs.html.scripts

/**
  * Mix in this trait to create the Orkestra job server.
  */
trait OrkestraServer extends OrkestraPlugin {
  private lazy val logger = Logger(getClass)
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  implicit override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  implicit override lazy val elasticsearchClient: HttpClient = Elasticsearch.client

  def jobs: Set[Job[_, _]]

  lazy val routes =
    pathPrefix("assets" / Remaining) { file =>
      encodeResponse {
        getFromResource(s"public/$file")
      }
    } ~
      pathSingleSlash {
        complete {
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            s"""<!DOCTYPE html>
               |<html>
               |<head>
               |    <title>${BuildInfo.projectName}</title>
               |    <link rel="icon" type="image/svg+xml" href="assets/logo.svg">
               |    <link rel="icon" type="image/png" href="assets/favicon.png">
               |    <meta name="basePath" content="${orkestraConfig.basePath}">
               |</head>
               |<body>
               |<div id="${BuildInfo.projectName.toLowerCase}"></div>
               |${scripts(
                 "web",
                 name => s"/assets/$name",
                 name => getClass.getResource(s"/public/$name") != null
               ).body}
               |</body>
               |</html>
               |""".stripMargin
          )
        }
      } ~
      pathPrefix(OrkestraConfig.apiSegment) {
        pathPrefix(OrkestraConfig.jobSegment) {
          jobs.map(_.apiRoute).reduce(_ ~ _)
        } ~
          path(OrkestraConfig.commonSegment / Segments) { segments =>
            entity(as[Json]) { json =>
              val body = AutowireServer.read[Map[String, Json]](json)
              val request = AutowireServer.route[CommonApi](CommonApiServer())(Core.Request(segments, body))
              onSuccess(request)(json => complete(json))
            }
          }
      }

  def main(args: Array[String]): Unit =
    Await.result(
      orkestraConfig.runInfoMaybe.fold {
        for {
          _ <- Future(logger.info("Initializing Elasticsearch"))
          _ <- Elasticsearch.init()
          _ = logger.info("Starting master Orkestra")
          _ <- onMasterStart()
          _ <- Http().bindAndHandle(routes, "0.0.0.0", orkestraConfig.port)
        } yield ()
      } { runInfo =>
        for {
          _ <- Future(logger.info(s"Running job $runInfo"))
          _ <- onJobStart(runInfo)
          _ <- jobs
            .find(_.board.id == runInfo.jobId)
            .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.jobId}"))
            .start(runInfo)
        } yield ()
      },
      Duration.Inf
    )
}
