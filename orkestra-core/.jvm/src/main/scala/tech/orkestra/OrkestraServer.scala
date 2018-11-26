package tech.orkestra

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{entity, _}
import autowire.Core
import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.http.ElasticClient
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.shapes._
import io.circe.java8.time._
import scalajs.html.scripts
import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.job.Job
import tech.orkestra.kubernetes.Kubernetes
import tech.orkestra.utils.{AutowireServer, Elasticsearch}

/**
  * Mix in this trait to create the Orkestra job server.
  */
trait OrkestraServer extends IOApp with OrkestraPlugin[IO] {
  private lazy val logger = Logger(getClass)
  override lazy val F: Applicative[IO] = implicitly[Applicative[IO]]
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  implicit override lazy val elasticsearchClient: ElasticClient = Elasticsearch.client

  def jobs: Set[Job[IO, _, _]]

  def routes(implicit kubernetesClient: KubernetesClient[IO]) =
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
                 name => Option(getClass.getResource(s"/public/$name")).isDefined
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
              val request = AutowireServer.route[CommonApi](CommonApiServer[IO]())(Core.Request(segments, body))
              onSuccess(request)(json => complete(json))
            }
          }
      }

  def run(args: List[String]): IO[ExitCode] = Kubernetes.client[IO].use { implicit kubernetesClient =>
    orkestraConfig.runInfoMaybe.fold {
      for {
        _ <- IO.pure(logger.info("Initializing Elasticsearch"))
        _ <- Elasticsearch.init[IO]
        _ = logger.info("Starting master Orkestra")
        _ <- onMasterStart(kubernetesClient)
        _ <- IO.fromFuture(IO(Http().bindAndHandle(routes, "0.0.0.0", orkestraConfig.port)))
      } yield ExitCode.Success
    } { runInfo =>
      for {
        _ <- IO.delay(logger.info(s"Running job $runInfo"))
        _ <- onJobStart(runInfo)
        _ <- jobs
          .find(_.board.id == runInfo.jobId)
          .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.jobId}"))
          .start(runInfo)
      } yield ExitCode.Success
    }
  }
}
