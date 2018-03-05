package io.chumps.orchestra.lock

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.{ElasticDate, Index, Minutes}
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.utils.AkkaImplicits._

private[lock] object Locks {
  case class Lock(updatedOn: Instant)

  val index = Index("locks")
  val `type` = "lock"

  def indexLockDoc(id: String) = indexInto(index, `type`).id(id).source(Lock(Instant.now()))

  def trylock(
    id: String
  )(implicit elasticsearchClient: HttpClient): Future[Either[RequestFailure, RequestSuccess[IndexResponse]]] =
    for {
      _ <- elasticsearchClient.execute(
        deleteByQuery(index, `type`, rangeQuery("updatedOn").lt(ElasticDate.now.minus(1, Minutes)))
      )
      createLock <- elasticsearchClient.execute(indexLockDoc(id).createOnly(true).refreshImmediately)
    } yield createLock

  def runLocked[Result](id: String)(f: => Result)(implicit elasticsearchClient: HttpClient): Future[Result] = {
    val keepLock =
      system.scheduler.schedule(30.seconds, 30.seconds)(elasticsearchClient.execute(indexLockDoc(id)))
    Future(f).transformWith { triedResult =>
      keepLock.cancel()
      elasticsearchClient
        .execute(deleteById(index, `type`, id).refreshImmediately)
        .flatMap(_ => Future.fromTry(triedResult))
    }
  }
}
