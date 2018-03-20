package com.drivetribe.orchestra.utils

import java.io.IOException
import java.time.Instant

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.HttpClient
import shapeless._

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.model.Indexed._
import com.drivetribe.orchestra.utils.AkkaImplicits._

trait Stages {
  protected def orchestraConfig: OrchestraConfig

  protected def elasticsearchClient: HttpClient

  def stage[Result](name: String) = StageBuilder(name)

  case class StageBuilder(name: String) {
    def apply[Result](func: => Result): Result = Await.result(apply(Future(func)), Duration.Inf)

    def apply[Result](func: => Future[Result]): Future[Result] =
      for {
        run <- elasticsearchClient
          .execute(get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(orchestraConfig.runInfo)))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result.to[Run[HNil, Unit]])

        stageStart = Stage(orchestraConfig.runInfo, run.parentJob, name, Instant.now(), Instant.now())
        stageIndexResponse <- elasticsearchClient
          .execute(indexInto(StagesIndex.index, StagesIndex.`type`).source(stageStart))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity))

        runningPong = system.scheduler.schedule(1.second, 1.second) {
          elasticsearchClient
            .execute(
              updateById(StagesIndex.index, StagesIndex.`type`, stageIndexResponse.result.id)
                .source(stageStart.copy(latestUpdateOn = Instant.now()))
            )
            .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
        }

        _ = println(s"Stage: $name")
        result <- func.transformWith { triedResult =>
          runningPong.cancel()
          Future.fromTry(triedResult)
        }
      } yield result
  }
}

object Stages extends Stages {
  override implicit lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val elasticsearchClient: HttpClient = Elasticsearch.client
}
