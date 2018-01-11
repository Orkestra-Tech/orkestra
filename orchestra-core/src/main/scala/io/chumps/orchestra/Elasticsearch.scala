package io.chumps.orchestra

import scala.concurrent.Future

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import cats.implicits._

import io.chumps.orchestra.model.Indexed
import io.chumps.orchestra.utils.AkkaImplicits._

object Elasticsearch {

  lazy val client: Future[HttpClient] = for {
    client <- Future(HttpClient(OrchestraConfig.elasticsearchUri))
    _ <- Indexed.indices.toList.traverse(index => client.execute(index.createDefinition))
  } yield client
}
