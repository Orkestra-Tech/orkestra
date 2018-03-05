package io.chumps.orchestra.lock

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.http.HttpClient

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.utils.Elasticsearch

sealed trait ElasticsearchLock {
  protected implicit val elasticsearchClient: HttpClient
  val id: String

  def orElse[Result](f: => Result)(orElse: => Result): Future[Result] =
    Locks.trylock(id).flatMap(_.fold(_ => Future(orElse), _ => Locks.runLocked(id)(f)))

  def orWait[Result](f: => Result): Future[Result] = {
    def retry(f: => Result, timeElapsed: FiniteDuration = 0.second): Future[Result] =
      Locks
        .trylock(id)
        .flatMap(
          _.fold(
            { _ =>
              val interval = 1.second
              if (timeElapsed.toSeconds % 1.minute.toSeconds == 1) println(s"Waiting for lock $id to be released")
              Thread.sleep(interval.toMillis)
              retry(f, timeElapsed + interval)
            },
            _ => Locks.runLocked(id)(f)
          )
        )

    retry(f)
  }
}

case class Lock(id: String) extends ElasticsearchLock {
  private implicit val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  protected val elasticsearchClient: HttpClient = Elasticsearch.client
}
