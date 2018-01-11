package io.chumps.orchestra.lock

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.{ElasticDate, Index, Minutes}
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.Elasticsearch
import io.chumps.orchestra.utils.AkkaImplicits._

case class Lock(id: String) {

  def orElse[Result](f: => Result)(orElse: => Result): Future[Result] =
    Lock.trylock(id).flatMap(_.fold(_ => Future(orElse), _ => Lock.runLocked(id)(f)))

  def orWait[Result](f: => Result): Future[Result] =
    Lock
      .trylock(id)
      .flatMap(_.fold({ _ =>
        println(s"Waiting for lock $id to be released")
        Thread.sleep(5.seconds.toMillis)
        orWait(f)
      }, _ => Lock.runLocked(id)(f)))
}

object Lock {
  private case class Lock(updatedOn: Instant)

  private val index = Index("locks")

  private def indexLockDoc(id: String) = indexInto(index, "lock").id(id).source(Lock(Instant.now()))

  private def trylock(id: String): Future[Either[RequestFailure, RequestSuccess[IndexResponse]]] =
    for {
      client <- Elasticsearch.client
      _ <- client.execute(
        deleteByQuery(index, "lock", rangeQuery("updatedOn").lt(ElasticDate.now.minus(1, Minutes)))
      )
      createLock <- client.execute(indexLockDoc(id).createOnly(true).refreshImmediately)
    } yield createLock

  private def runLocked[Result](id: String)(f: => Result): Future[Result] = {
    val keepLock =
      system.scheduler.schedule(30.seconds, 30.seconds)(Elasticsearch.client.map(_.execute(indexLockDoc(id))))
    val result = Future(f)
    result.onComplete { _ =>
      keepLock.cancel()
      Elasticsearch.client.map(_.execute(deleteById(index, "lock", id).refreshImmediately))
    }
    result
  }
}
