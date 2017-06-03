package com.goyeau.orchestra.routes

import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.ActorMaterializer
import com.goyeau.orchestra.Job.Runner

case class Backend(jobs: Seq[Runner[_, _, _, _]]) extends HttpApp {
  implicit lazy val actorSystem = systemReference.get
  implicit lazy val executionContext = actorSystem.dispatcher
  implicit lazy val materializer = ActorMaterializer()

  lazy val route: Route =
    pathSingleSlash {
      complete {
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          s"""<!DOCTYPE html>
             |<html>
             |<head>
             |    <title>Orchestra</title>
             |</head>
             |<body>
             |<div id="orchestra"></div>
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
      pathPrefix("assets" / Remaining) { file =>
        // optionally compresses the response with Gzip or Deflate
        // if the client accepts compressed responses
        encodeResponse {
          getFromResource(s"public/$file")
        }
      } ~
      pathPrefix("api") {
        jobs.map(_.apiRoute).reduce(_ ~ _)
      }
}
