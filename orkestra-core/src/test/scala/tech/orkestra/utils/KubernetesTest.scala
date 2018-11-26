package tech.orkestra.utils

import scala.concurrent.duration._
import cats.implicits._
import cats.effect.{ConcurrentEffect, Resource}
import com.goyeau.kubernetes.client.{KubeConfig, KubernetesClient}
import io.circe.generic.auto._
import io.k8s.api.batch.v1.{Job, JobList}
import io.k8s.api.batch.v1beta1.{CronJob, CronJobList}
import io.k8s.api.core.v1.{Container, Pod, PodSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._

trait KubernetesTest[F[_]] extends BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures with Http4sDsl[F] {
  self: Suite with OrkestraConfigTest =>

  implicit def F: ConcurrentEffect[F]
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)

  val kubeClient: KubernetesClient[F] =
    KubernetesClient(Client.fromHttpApp(routes), KubeConfig(orkestraConfig.kubeUri))

  implicit val kubernetesClient: Resource[F, KubernetesClient[F]] = Resource.pure(kubeClient)

  def usingKubernetesClient[T](body: KubernetesClient[F] => F[T]): T =
    ConcurrentEffect[F].toIO(body(kubeClient)).unsafeRunSync()

  private var jobs = Map.empty[String, Job] // scalafix:ok
  private var cronJobs = Map.empty[String, CronJob] // scalafix:ok
  private lazy val routes = HttpRoutes
    .of[F] {
      case request @ POST -> Root / "apis" / "batch" / "v1beta1" / "namespaces" / orkestraConfig.namespace / "cronjobs" =>
        request.as[CronJob].flatMap { cronJob =>
          if (cronJobs.contains(cronJob.metadata.get.name.get)) Conflict()
          else {
            cronJobs += cronJob.metadata.get.name.get -> cronJob
            Ok()
          }
        }
      case GET -> Root / "apis" / "batch" / "v1beta1" / "namespaces" / orkestraConfig.namespace / "cronjobs" =>
        Ok(CronJobList(cronJobs.values.toSeq))
      case DELETE -> Root / "apis" / "batch" / "v1beta1" / "namespaces" / orkestraConfig.namespace / "cronjobs" / cronJobName =>
        cronJobs -= cronJobName
        Ok()

      case request @ POST -> Root / "apis" / "batch" / "v1" / "namespaces" / orkestraConfig.namespace / "jobs" =>
        request.as[Job].flatMap { job =>
          if (jobs.contains(job.metadata.get.name.get)) Conflict()
          else {
            jobs += job.metadata.get.name.get -> job
            Ok()
          }
        }
      case GET -> Root / "apis" / "batch" / "v1" / "namespaces" / orkestraConfig.namespace / "jobs" =>
        Ok(JobList(jobs.values.toSeq))
      case DELETE -> Root / "apis" / "batch" / "v1" / "namespaces" / orkestraConfig.namespace / "jobs" / jobName =>
        jobs -= jobName
        Ok()

      case GET -> Root / "api" / "v1" / "namespaces" / orkestraConfig.namespace / "pods" / orkestraConfig.podName =>
        Ok(
          Pod(
            metadata = Option(ObjectMeta(name = Option(orkestraConfig.podName))),
            spec = Option(PodSpec(containers = Seq(Container(name = "orkestra"))))
          )
        )
    }
    .orNotFound

//  private val routes =
//    pathPrefix("apis" / "batch") {
//      pathPrefix("v1beta1" / "namespaces" / orkestraConfig.namespace / "cronjobs") {
//        pathEndOrSingleSlash {
//          get {
//            complete(CronJobList(cronJobs.values.toSeq).asJson.noSpaces)
//          } ~
//            post {
//              entity(as[String]) { entity =>
//                complete {
//                  val cronJob = decode[CronJob](entity).fold(throw _, identity)
//                  if (cronJobs.contains(cronJob.metadata.get.name.get)) Conflict
//                  else {
//                    cronJobs += cronJob.metadata.get.name.get -> cronJob
//                    OK
//                  }
//                }
//              }
//            }
//        } ~
//          path(Segment) { cronJobName =>
//            patch {
//              entity(as[String]) { entity =>
//                complete {
//                  cronJobs += cronJobName -> decode[CronJob](entity).fold(throw _, identity)
//                  OK
//                }
//              }
//            } ~
//              get {
//                cronJobs.get(cronJobName) match {
//                  case Some(cronJob) => complete(cronJob.asJson.noSpaces)
//                  case None          => complete(NotFound)
//                }
//              } ~
//              delete {
//                complete {
//                  cronJobs -= cronJobName
//                  OK
//                }
//              }
//          }
//      } ~
//        pathPrefix("v1" / "namespaces" / orkestraConfig.namespace / "jobs") {
//          pathEndOrSingleSlash {
//            get {
//              complete(JobList(jobs.values.toSeq).asJson.noSpaces)
//            } ~
//              post {
//                entity(as[String]) { entity =>
//                  complete {
//                    val job = decode[Job](entity).fold(throw _, identity)
//                    if (cronJobs.contains(job.metadata.get.name.get)) Conflict
//                    else {
//                      jobs += job.metadata.get.name.get -> job
//                      OK
//                    }
//                  }
//                }
//              }
//          } ~
//            path(Segment) { jobName =>
//              get {
//                jobs.get(jobName) match {
//                  case Some(job) => complete(job.asJson.noSpaces)
//                  case None      => complete(NotFound)
//                }
//              } ~
//                delete {
//                  complete {
//                    jobs -= jobName
//                    OK
//                  }
//                }
//            }
//        }
//    } ~
//      pathPrefix("api" / "v1" / "namespaces" / orkestraConfig.namespace / "pods" / orkestraConfig.podName) {
//        pathEndOrSingleSlash {
//          complete(
//            Pod(
//              metadata = Option(ObjectMeta(name = Option(orkestraConfig.podName))),
//              spec = Option(PodSpec(containers = Seq(Container(name = "orkestra"))))
//            ).asJson.noSpaces
//          )
//        } ~
//          path("exec") {
//            val helloer = Flow.fromSinkAndSourceMat(Sink.ignore, Source.single(TextMessage("\nHello")))(Keep.right)
//            handleWebSocketMessagesForProtocol(helloer, "v4.channel.k8s.io")
//          }
//      }

  override def beforeEach(): Unit = {
    super.beforeEach()
    jobs = Map.empty
    cronJobs = Map.empty
  }

//  override def beforeAll(): Unit = {
//    super.beforeAll()
//    Http().bindAndHandle(routes, "0.0.0.0", kubernetesApiPort)
//    Thread.sleep(1.second.toMillis)
//  }
}
