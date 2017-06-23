package com.goyeau.orchestra.github

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.Materializer
import com.goyeau.orchestra.{JVMApp, OrchestraConfig}
import io.circe.parser._

trait Github extends JVMApp {
  implicit def actorSystem: ActorSystem
  implicit def materializer: Materializer
  implicit def executionContext: ExecutionContext

  def githubTriggers: Seq[BranchTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    lazy val routes =
      pathPrefix("webhooks") {
        post {
          headerValueByName("X-GitHub-Event") { eventType =>
            entity(as[String]) { entity =>
              val json = parse(entity).fold(throw _, identity)
              githubTriggers.foreach(_.trigger(eventType, json))
              complete(OK)
            }
          }
        }
      }

    if (OrchestraConfig.runInfo.isEmpty) {
      val port =
        OrchestraConfig.githubPort.getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_PORT should be set"))
      Http().bindAndHandle(routes, "0.0.0.0", port)
    }
  }
}
