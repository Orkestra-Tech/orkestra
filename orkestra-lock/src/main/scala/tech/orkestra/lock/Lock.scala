package tech.orkestra.lock

import scala.concurrent.Future
import scala.concurrent.duration._
import com.sksamuel.elastic4s.http.ElasticClient
import tech.orkestra.OrkestraConfig
import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.utils.Elasticsearch

sealed trait ElasticsearchLock {
  implicit protected val elasticsearchClient: ElasticClient
  val id: String

  /**
    * Try lock, if acquired then run func else run or.
    *
    * @param func The function to run if the lock has been acquired
    * @param or The function to run if the lock has not been acquired
    */
  def orElse[Result](func: => Result)(or: => Result): Future[Result] = orElse(Future(func))(Future(or))

  /**
    * Try lock, if acquired then run func else run or.
    *
    * @param func The function to run if the lock has been acquired
    * @param or The function to run if the lock has not been acquired
    */
  def orElse[Result](func: => Future[Result])(or: => Future[Result])(implicit dummy: DummyImplicit): Future[Result] =
    Locks.trylock(id).flatMap(_.fold(or)(_ => Locks.runLocked(id, func)))

  /**
    * Try lock. This awaits asynchronously until the lock is released to run func.
    *
    * @param func The function to run once the lock has been acquired
    */
  def orWait[Result](func: => Result): Future[Result] = orWait(Future(func))

  /**
    * Try lock. This awaits asynchronously until the lock is released to run func.
    *
    * @param func The function to run once the lock has been acquired
    */
  def orWait[Result](func: => Future[Result])(implicit dummy: DummyImplicit): Future[Result] = {
    def retry(func: => Future[Result], timeElapsed: FiniteDuration = 0.second): Future[Result] =
      Locks
        .trylock(id)
        .flatMap(
          _.fold {
            val interval = 1.second
            if (timeElapsed.toSeconds % 1.minute.toSeconds == 1) println(s"Waiting for lock $id to be released")
            Thread.sleep(interval.toMillis)
            retry(func, timeElapsed + interval)
          }(_ => Locks.runLocked(id, func))
        )

    retry(func)
  }
}

/**
  * Create a lock
  *
  * @param id The id of the lock
  */
case class Lock(id: String) extends ElasticsearchLock {
  implicit private val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  protected val elasticsearchClient: ElasticClient = Elasticsearch.client
}
