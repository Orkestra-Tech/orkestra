package com.goyeau.orchestra

import scala.scalajs.js.JSApp
import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, pathSingleSlash, _}
import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.route.{BackendRoutes, WebRouter}
import org.scalajs.dom

trait Boards extends BackendRoutes with JSApp {

  def board: Board

  lazy val topElementId = "orchestra"

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
             |    <title>Orchestra</title>
             |</head>
             |<body>
             |<div id="$topElementId"></div>
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

  def main(): Unit = {
    AppCSS.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(topElementId))
  }
}
