package com.goyeau.orchestra

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.goyeau.orchestra.route.BackendRoutes
import com.goyeau.orchestra.Implicits._

trait Jobs extends JVMApp with BackendRoutes {

  def jobs: Seq[Job.Runner[_, _, _, _]]

  override lazy val routes = super.routes ~
    pathPrefix("assets" / Remaining) { file =>
      encodeResponse {
        getFromResource(s"public/$file")
      }
    } ~
    pathPrefix("api") {
      jobs.map(_.apiRoute).reduce(_ ~ _)
    }

  override def main(args: Array[String]): Unit = {
    super.main(args)

    Config.runInfo.fold[Unit] {
      val port = Config.port.getOrElse(throw new IllegalStateException("ORCHESTRA_PORT should be set"))
      Http().bindAndHandle(routes, "0.0.0.0", port)
    } { runInfo =>
      jobs
        .find(_.definition.id == runInfo.jobId)
        .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.jobId}"))
        .run(runInfo)
    }
  }
}
