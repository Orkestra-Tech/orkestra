package com.drivetribe.orchestra.utils

import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.goyeau.kubernetesclient.{KubeConfig, KubernetesClient}
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import io.k8s.api.batch.v1.{Job, JobList}
import io.k8s.api.core.v1.{Container, Pod, PodSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import com.drivetribe.orchestra.utils.AkkaImplicits._

trait KubernetesTest extends BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  self: Suite with OrchestraConfigTest =>
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)
  implicit val kubernetesClient: KubernetesClient = KubernetesClient(KubeConfig(orchestraConfig.kubeUri))

  private var runningKubeJobs = Seq.empty[Job]
  private val routes =
    pathPrefix("apis" / "batch" / "v1" / "namespaces" / orchestraConfig.namespace / "jobs") {
      pathEndOrSingleSlash {
        get {
          complete(
            OK, {
              val jobList = JobList(runningKubeJobs)
              jobList.asJson // For some reason we need to call .asJson twice
              jobList.asJson.noSpaces
            }
          )
        } ~
          post {
            entity(as[String]) { entity =>
              complete {
                runningKubeJobs :+= decode[Job](entity).fold(throw _, identity)
                OK
              }
            }
          }
      } ~
        path(Segment) { jobName =>
          delete {
            complete {
              runningKubeJobs = runningKubeJobs.filterNot(_.metadata.get.name.get == jobName)
              OK
            }
          }
        }
    } ~
      pathPrefix("api" / "v1" / "namespaces" / orchestraConfig.namespace / "pods" / orchestraConfig.podName) {
        pathEndOrSingleSlash {
          complete(
            OK,
            Pod(
              metadata = Option(ObjectMeta(name = Option(orchestraConfig.podName))),
              spec = Option(PodSpec(containers = Seq(Container(name = "orchestration"))))
            ).asJson.noSpaces
          )
        } ~
          path("exec") {
            val helloer = Flow.fromSinkAndSourceMat(Sink.ignore, Source.single(TextMessage("\nHello")))(Keep.right)
            handleWebSocketMessagesForProtocol(helloer, "v4.channel.k8s.io")
          }
      }

  override def beforeEach(): Unit = {
    super.beforeEach()
    runningKubeJobs = Seq.empty
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Http().bindAndHandle(routes, "0.0.0.0", kubernetesApiPort)
    Thread.sleep(1.second.toMillis)
  }
}
