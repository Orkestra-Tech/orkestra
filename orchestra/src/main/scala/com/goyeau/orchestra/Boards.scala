package com.goyeau.orchestra

import scala.scalajs.js.JSApp
import scalajs.html.scripts

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, pathSingleSlash, _}
import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.{BackendRoutes, WebRouter}
import io.circe.shapes.HListInstances
import org.scalajs.dom

trait Boards extends BackendRoutes with JSApp {
  def board: Board

  override lazy val routes = super.routes ~
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
    }

  override def main(): Unit = {
    AppCSS.load
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById("orchestra"))
  }
}
