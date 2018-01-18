package io.chumps.orchestra.utils

import java.io.IOException
import java.time.Instant

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.DynamicVariable

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.{Elasticsearch, OrchestraConfig}
import io.chumps.orchestra.model.Indexed.StagesIndex
import io.chumps.orchestra.model.Indexed.Stage
import io.chumps.orchestra.utils.AkkaImplicits._

trait StagesHelpers {

  def stage[Result](name: String)(f: => Result): Result =
    Await.result(
      for {
        stageStart <- Future.successful(Stage(OrchestraConfig.runInfo.runId, name, Instant.now(), None))
        stageIndexResponse <- Elasticsearch.client
          .execute(indexInto(StagesIndex.index, StagesIndex.`type`).source(stageStart))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity))
        result <- Future(StagesHelpers.stageVar.withValue(Option(name)) {
          println(s"Stage: $name")
          f
        }).transformWith { triedResult =>
          Elasticsearch.client
            .execute(
              updateById(
                StagesIndex.index.name + "/" + StagesIndex.`type`, // TODO: Remove workaround when fixed in elastic4s
                StagesIndex.`type`,
                stageIndexResponse.result.id
              ).source(stageStart.copy(completedOn = Option(Instant.now())))
            )
            .flatMap(_ => Future.fromTry(triedResult))
        }
      } yield result,
      Duration.Inf
    )
}

object StagesHelpers {
  private[orchestra] val stageVar = new DynamicVariable[Option[String]](None)
}
