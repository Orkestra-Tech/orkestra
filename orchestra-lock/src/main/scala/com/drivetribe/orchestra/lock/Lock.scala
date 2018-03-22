package com.drivetribe.orchestra.lock

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.http.HttpClient

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.drivetribe.orchestra.utils.Elasticsearch

sealed trait ElasticsearchLock {
  protected implicit val elasticsearchClient: HttpClient
  val id: String

  def orElse[Result](func: => Result)(or: => Result): Future[Result] = orElse(Future(func))(Future(or))
  def orElse[Result](func: => Future[Result])(or: => Future[Result])(implicit dummy: DummyImplicit): Future[Result] =
    Locks.trylock(id).flatMap(_.fold(_ => or, _ => Locks.runLocked(id, func)))

  def orWait[Result](func: => Result): Future[Result] = orWait(Future(func))
  def orWait[Result](func: => Future[Result])(implicit dummy: DummyImplicit): Future[Result] = {
    def retry(func: => Future[Result], timeElapsed: FiniteDuration = 0.second): Future[Result] =
      Locks
        .trylock(id)
        .flatMap(
          _.fold(
            { _ =>
              val interval = 1.second
              if (timeElapsed.toSeconds % 1.minute.toSeconds == 1) println(s"Waiting for lock $id to be released")
              Thread.sleep(interval.toMillis)
              retry(func, timeElapsed + interval)
            },
            _ => Locks.runLocked(id, func)
          )
        )

    retry(func)
  }
}

case class Lock(id: String) extends ElasticsearchLock {
  private implicit val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  protected val elasticsearchClient: HttpClient = Elasticsearch.client
}
