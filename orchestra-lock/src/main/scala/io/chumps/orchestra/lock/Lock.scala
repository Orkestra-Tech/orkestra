package io.chumps.orchestra.lock

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.{ElasticDate, ElasticsearchClientUri, Index, Minutes}
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure}
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.utils.AkkaImplicits._

case class Lock(id: String) {

  def orElse[Result](f: => Result)(orElse: => Result): Future[Result] =
    Lock.trylock(id)(f).map(_.getOrElse(orElse))

  def orWait[Result](f: => Result): Future[Result] =
    Lock
      .trylock(id)(f)
      .flatMap(_.fold({ _ =>
        println(s"Waiting for lock $id to be released")
        Thread.sleep(5.seconds.toMillis)
        orWait(f)
      }, Future(_)))
}

object Lock {
  private case class Lock(createdOn: Instant)

  private val client = HttpClient(ElasticsearchClientUri(OrchestraConfig.elasticsearchUri))
  private val index = Index("locks")

  private def indexLockDoc(id: String) = indexInto(Index("locks"), "lock").id(id).source(Lock(Instant.now()))

  private def trylock[Result](id: String)(f: => Result): Future[Either[RequestFailure, Result]] =
    (
      for {
        _ <- client.execute(
          deleteByQuery(index, "lock", rangeQuery("createdOn").lt(ElasticDate.now.minus(1, Minutes)))
        )
        createLock <- client.execute(indexLockDoc(id).createOnly(true).refreshImmediately)
        keepLock = system.scheduler.schedule(30.seconds, 30.seconds)(client.execute(indexLockDoc(id)))
      } yield
        try createLock.map(_ => f)
        finally keepLock.cancel()
    ).andThen {
      case _ => client.execute(deleteById(index, "lock", id).refreshImmediately)
    }
}
