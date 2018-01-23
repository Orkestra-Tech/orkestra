package io.chumps.orchestra

import scala.concurrent.Future

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient

import io.chumps.orchestra.model.Indexed
import io.chumps.orchestra.utils.AkkaImplicits._

object Elasticsearch {

  lazy val client = HttpClient(OrchestraConfig.elasticsearchUri)

  def init() = Future.traverse(Indexed.indices)(index => client.execute(index.createDefinition))
}
