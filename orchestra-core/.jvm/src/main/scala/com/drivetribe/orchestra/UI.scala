package com.drivetribe.orchestra

import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import com.drivetribe.orchestra.route.BackendRoutes

/**
  * Mix in this trait to add a web UI to Orchestra
  */
trait UI extends BackendRoutes { self: Orchestra =>
  protected implicit val orchestraConfig: OrchestraConfig

  override lazy val routes = super.routes ~
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
             |    <meta name="basePath" content="${orchestraConfig.basePath}">
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
    }
}
