package io.chumps.orchestra

import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import io.chumps.orchestra.route.BackendRoutes

trait UI extends BackendRoutes {
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
