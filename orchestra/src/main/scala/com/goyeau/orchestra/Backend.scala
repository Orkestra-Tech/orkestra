package com.goyeau.orchestra

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{HttpApp, Route}
import scalajs.html.scripts

case class Backend(board: Board) extends HttpApp {
  implicit lazy val executionContext = systemReference.get().dispatcher

  def route: Route =
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
      path("api" / Segments) { segments =>
        post(AutowireServer.dispatch(segments))
      }
}
