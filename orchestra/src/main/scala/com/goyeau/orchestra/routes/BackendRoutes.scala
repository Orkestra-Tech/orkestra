package com.goyeau.orchestra.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives

trait BackendRoutes {
  protected def routes: Route = RouteDirectives.reject
}
