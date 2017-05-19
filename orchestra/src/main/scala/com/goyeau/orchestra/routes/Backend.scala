package com.goyeau.orchestra.routes

import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{HttpApp, Route}
import com.goyeau.orchestra.Task.Runner

case class Backend(tasks: Seq[Runner[_, _, _]]) extends HttpApp {
  implicit lazy val executionContext = systemReference.get.dispatcher

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
        tasks.map(_.apiRoute).reduce(_ ~ _)
      }
}
