package io.chumps.orchestra.utils

import scala.concurrent.Future
import scala.concurrent.duration._

import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.admin.RefreshIndexResponse
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.testkit.AlwaysNewLocalNodeProvider
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import io.chumps.orchestra.model.Indexed
import io.chumps.orchestra.utils.AkkaImplicits._

trait ElasticsearchTest
    extends AlwaysNewLocalNodeProvider
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually { self: Suite =>
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  // https://discuss.elastic.co/t/elasticsearch-5-4-1-availableprocessors-is-already-set/88036
  System.setProperty("es.set.netty.runtime.available.processors", false.toString)
  implicit val elasticsearchClient: _root_.com.sksamuel.elastic4s.http.HttpClient = http

  override def beforeAll(): Unit = {
    super.beforeAll()
    (for {
      _ <- elasticsearchClient.execute(deleteIndex(Indexes.All.values))
      _ <- Elasticsearch.init()
    } yield ()).futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    http.close()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    (for {
      _ <- Future.traverse(Indexed.indices) { indexDef =>
        elasticsearchClient.execute(deleteByQuery(indexDef.index, indexDef.`type`, matchAllQuery()).refreshImmediately)
      }
      _ <- refreshIndices()
    } yield ()).futureValue
  }

  def refreshIndices(): Future[Either[RequestFailure, RequestSuccess[RefreshIndexResponse]]] =
    elasticsearchClient.execute(refreshIndex(Indexes.All))
}
