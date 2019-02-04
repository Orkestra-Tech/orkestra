//package tech.orkestra.integration.tests.utils
//
//import scala.concurrent.Future
//import scala.concurrent.duration._
//
//import akka.http.scaladsl.model.{ContentTypes, HttpMethods}
//import io.circe.Json
//import io.k8s.apimachinery.pkg.apis.meta.v1.DeleteOptions
//import org.scalatest._
//import org.scalatest.concurrent.{Eventually, ScalaFutures}
//
//import tech.orkestra.model.Indexed
//import tech.orkestra.utils.AkkaImplicits._
//
//trait IntegrationTest extends BeforeAndAfter with BeforeAndAfterAll with ScalaFutures with Eventually { this: Suite =>
//  implicit override val patienceConfig = PatienceConfig(timeout = 1.minute, interval = 500.millis)
//
//  override def beforeAll() = {
//    super.beforeAll()
//    (for {
//      _ <- DeployElasticsearch(Kubernetes.client)
//      _ <- Deployorkestra(Kubernetes.client)
//    } yield ()).futureValue(timeout(5.minutes))
//  }
//
//  override def afterAll() = {
//    super.afterAll()
//    Kubernetes.client.namespaces.delete(Kubernetes.namespace.metadata.get.name.get).futureValue(timeout(1.minute))
//  }
//
//  before {
//    (for {
//      _ <- stopRunningJobs()
//      _ <- emptyElasticsearch()
//    } yield ()).futureValue
//  }
//
//  private def stopRunningJobs() = {
//    def awaitNoJobRunning(): Future[Unit] =
//      for {
//        jobs <- Kubernetes.client.jobs.list()
//        _ <- if (jobs.items.isEmpty) Future.unit else awaitNoJobRunning()
//      } yield ()
//
//    for {
//      jobs <- Kubernetes.client.jobs.list()
//      _ <- Future.traverse(jobs.items) { job =>
//        Kubernetes.client.jobs
//          .namespace(Kubernetes.namespace.metadata.get.name.get)
//          .delete(
//            job.metadata.get.name.get,
//            Option(DeleteOptions(propagationPolicy = Option("Foreground"), gracePeriodSeconds = Option(0)))
//          )
//      }
//      _ <- awaitNoJobRunning()
//    } yield ()
//  }
//
//  private def emptyElasticsearch() = Future.traverse(Indexed.indices) { indexDef =>
//    Kubernetes.client.services
//      .namespace(Kubernetes.namespace.metadata.get.name.get)
//      .proxy(
//        DeployElasticsearch.service.metadata.get.name.get,
//        HttpMethods.POST,
//        s"/${indexDef.index.name}/_delete_by_query",
//        ContentTypes.`application/json`,
//        Option(Json.obj("query" -> Json.obj("match_all" -> Json.obj())).noSpaces)
//      )
//  }
//}
