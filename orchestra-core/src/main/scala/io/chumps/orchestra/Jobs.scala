package io.chumps.orchestra

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{entity, _}
import autowire.Core
import io.circe.java8.time._

import io.chumps.orchestra.route.BackendRoutes
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.job.JobRunner

trait Jobs extends JVMApp with BackendRoutes {

  def jobRunners: Set[JobRunner[_, _]]

  override lazy val routes = super.routes ~
    pathPrefix(Jobs.apiSegment) {
      pathPrefix(Jobs.jobSegment) {
        jobRunners.map(_.apiRoute).reduce(_ ~ _)
      } ~
        path(Jobs.commonSegment / Segments) { segments =>
          entity(as[String]) { entity =>
            val body = AutowireServer.read[Map[String, String]](entity)
            val request = AutowireServer.route[CommonApi](CommonApiServer)(Core.Request(segments, body))
            onSuccess(request)(complete(_))
          }
        }
    }

  override def main(args: Array[String]): Unit = {
    super.main(args)

    Await.result(Elasticsearch.init(), 1.minute)

    OrchestraConfig.runInfoMaybe.fold[Unit] {
      Http().bindAndHandle(routes, "0.0.0.0", OrchestraConfig.port)
    } { runInfo =>
      jobRunners
        .find(_.job.id == runInfo.jobId)
        .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.jobId}"))
        .run(runInfo)
    }
  }
}

object Jobs {
  val apiSegment = "api"
  val jobSegment = "job"
  val commonSegment = "common"
}
