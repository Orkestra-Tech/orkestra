package tech.orkestra.utils

import java.io.IOException
import java.time.Instant

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticClient
import shapeless._
import tech.orkestra.OrkestraConfig
import tech.orkestra.model.Indexed._
import tech.orkestra.utils.AkkaImplicits._

trait Stages {
  protected def orkestraConfig: OrkestraConfig
  protected def elasticsearchClient: ElasticClient

  /**
    * Create a stage.
    * Name the execution of a part of the code and time it.
    *
    * @param name The name of the stage
    */
  def stage(name: String) = StageBuilder(name)

  case class StageBuilder(name: String) {
    def apply[Result](func: => Result): Result = Await.result(apply(Future(func)), Duration.Inf)

    def apply[Result](func: => Future[Result]): Future[Result] =
      for {
        run <- elasticsearchClient
          .execute(get(HistoryIndex.index, HistoryIndex.`type`, HistoryIndex.formatId(orkestraConfig.runInfo)))
          .map(response => response.fold(throw new IOException(response.error.reason))(_.to[Run[HNil, Unit]]))

        stageStart = Stage(orkestraConfig.runInfo, run.parentJob, name, Instant.now(), Instant.now())
        stageIndexResponse <- elasticsearchClient
          .execute(indexInto(StagesIndex.index, StagesIndex.`type`).source(stageStart))
          .map(response => response.fold(throw new IOException(response.error.reason))(identity))

        runningPong = system.scheduler.schedule(1.second, 1.second) {
          elasticsearchClient
            .execute(
              updateById(StagesIndex.index, StagesIndex.`type`, stageIndexResponse.id)
                .source(stageStart.copy(latestUpdateOn = Instant.now()))
            )
            .map(response => response.fold(throw new IOException(response.error.reason))(identity))
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
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val elasticsearchClient: ElasticClient = Elasticsearch.client
}
