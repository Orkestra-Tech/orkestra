package tech.orkestra.utils

import scala.concurrent.duration._
import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import tech.orkestra.utils.AkkaImplicits._

trait ElasticsearchTest extends BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures { self: Suite =>
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  // https://discuss.elastic.co/t/elasticsearch-5-4-1-availableprocessors-is-already-set/88036
  System.setProperty("es.set.netty.runtime.available.processors", false.toString)
  implicit val elasticsearchClient: ElasticClient = ElasticClient(ElasticProperties("http://localhost:9200"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    (for {
      _ <- elasticsearchClient.execute(deleteIndex(Indexes.All.values))
      _ <- Elasticsearch.init()
    } yield ()).futureValue
  }

  override def afterAll(): Unit = {
    super.afterAll()
    elasticsearchClient.close()
  }
}
