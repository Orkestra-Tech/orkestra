package tech.orkestra.utils

import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import tech.orkestra.utils.AkkaImplicits._
import com.goyeau.kubernetes.client.{KubeConfig, KubernetesClient}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.k8s.api.batch.v1.{Job, JobList}
import io.k8s.api.batch.v1beta1.{CronJob, CronJobList}
import io.k8s.api.core.v1.{Container, Pod, PodSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait KubernetesTest extends BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  self: Suite with OrkestraConfigTest =>
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)
  implicit val kubernetesClient: KubernetesClient = KubernetesClient(KubeConfig(orkestraConfig.kubeUri))

  private var jobs = Map.empty[String, Job] // scalafix:ok
  private var cronJobs = Map.empty[String, CronJob] // scalafix:ok
  private val routes =
    pathPrefix("apis" / "batch") {
      pathPrefix("v1beta1" / "namespaces" / orkestraConfig.namespace / "cronjobs") {
        pathEndOrSingleSlash {
          get {
            complete(CronJobList(cronJobs.values.toSeq).asJson.noSpaces)
          } ~
            post {
              entity(as[String]) { entity =>
                complete {
                  val cronJob = decode[CronJob](entity).fold(throw _, identity)
                  if (cronJobs.contains(cronJob.metadata.get.name.get)) Conflict
                  else {
                    cronJobs += cronJob.metadata.get.name.get -> cronJob
                    OK
                  }
                }
              }
            }
        } ~
          path(Segment) { cronJobName =>
            patch {
              entity(as[String]) { entity =>
                complete {
                  cronJobs += cronJobName -> decode[CronJob](entity).fold(throw _, identity)
                  OK
                }
              }
            } ~
              get {
                cronJobs.get(cronJobName) match {
                  case Some(cronJob) => complete(cronJob.asJson.noSpaces)
                  case None          => complete(NotFound)
                }
              } ~
              delete {
                complete {
                  cronJobs -= cronJobName
                  OK
                }
              }
          }
      } ~
        pathPrefix("v1" / "namespaces" / orkestraConfig.namespace / "jobs") {
          pathEndOrSingleSlash {
            get {
              complete(JobList(jobs.values.toSeq).asJson.noSpaces)
            } ~
              post {
                entity(as[String]) { entity =>
                  complete {
                    val job = decode[Job](entity).fold(throw _, identity)
                    if (cronJobs.contains(job.metadata.get.name.get)) Conflict
                    else {
                      jobs += job.metadata.get.name.get -> job
                      OK
                    }
                  }
                }
              }
          } ~
            path(Segment) { jobName =>
              get {
                jobs.get(jobName) match {
                  case Some(job) => complete(job.asJson.noSpaces)
                  case None      => complete(NotFound)
                }
              } ~
                delete {
                  complete {
                    jobs -= jobName
                    OK
                  }
                }
            }
        }
    } ~
      pathPrefix("api" / "v1" / "namespaces" / orkestraConfig.namespace / "pods" / orkestraConfig.podName) {
        pathEndOrSingleSlash {
          complete(
            Pod(
              metadata = Option(ObjectMeta(name = Option(orkestraConfig.podName))),
              spec = Option(PodSpec(containers = Seq(Container(name = "orkestra"))))
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
    jobs = Map.empty
    cronJobs = Map.empty
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Http().bindAndHandle(routes, "0.0.0.0", kubernetesApiPort)
    Thread.sleep(1.second.toMillis)
  }
}
