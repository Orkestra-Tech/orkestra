package io.chumps.orchestra.github

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{entity, _}
import io.chumps.orchestra.{JVMApp, OrchestraConfig}
import io.chumps.orchestra.AkkaImplicits._
import io.circe.parser._

trait Github extends JVMApp {

  def githubTriggers: Seq[GithubTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    lazy val routes =
      path("health") {
        complete(OK)
      } ~
        path("webhooks") {
          headerValueByName("X-GitHub-Event") { eventType =>
            entity(as[String]) { entity =>
              val json = parse(entity).fold(throw _, identity)
              githubTriggers.foreach(_.trigger(eventType, json))
              complete(OK)
            }
          }
        }

    if (OrchestraConfig.runInfo.isEmpty) Http().bindAndHandle(routes, "0.0.0.0", OrchestraConfig.githubPort)
  }
}
