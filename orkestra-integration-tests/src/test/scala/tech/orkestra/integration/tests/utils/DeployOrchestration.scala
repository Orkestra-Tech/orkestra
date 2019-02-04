//package tech.orkestra.integration.tests.utils
//
//import scala.concurrent.Future
//import scala.concurrent.duration._
//
//import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
//import com.goyeau.kubernetes.client.{IntValue, KubernetesClient, KubernetesException}
//import io.k8s.api.apps.v1beta2.{Deployment, DeploymentSpec}
//import io.k8s.api.core.v1._
//import io.k8s.apimachinery.pkg.apis.meta.v1.{LabelSelector, ObjectMeta}
//
//import tech.orkestra.integration.tests.BuildInfo
//import tech.orkestra.utils.AkkaImplicits._
//
//object Deployorkestra {
//  val apporkestraLabel = Option(Map("app" -> "orkestra"))
//
//  val service = Service(
//    metadata = Option(ObjectMeta(name = Option("orkestra"))),
//    spec = Option(
//      ServiceSpec(
//        selector = apporkestraLabel,
//        ports = Option(Seq(ServicePort(port = 80, targetPort = Option(IntValue(8080)))))
//      )
//    )
//  )
//
//  val deployment = Deployment(
//    metadata = Option(ObjectMeta(name = Option("orchestation"))),
//    spec = Option(
//      DeploymentSpec(
//        replicas = Option(1),
//        selector = Option(LabelSelector(matchLabels = apporkestraLabel)),
//        template = PodTemplateSpec(
//          metadata = Option(ObjectMeta(labels = apporkestraLabel)),
//          spec = Option(
//            PodSpec(
//              containers = Seq(
//                Container(
//                  name = "orkestra",
//                  image = Option(s"${BuildInfo.artifactName}:${BuildInfo.version}"),
//                  imagePullPolicy = Option("IfNotPresent"),
//                  env = Option(
//                    Seq(
//                      EnvVar(name = "ORKESTRA_KUBE_URI", value = Option("https://kubernetes.default")),
//                      EnvVar(
//                        name = "ORKESTRA_ELASTICSEARCH_URI",
//                        value = Option("elasticsearch://elasticsearch:9200")
//                      ),
//                      EnvVar(
//                        name = "ORKESTRA_POD_NAME",
//                        valueFrom = Option(
//                          EnvVarSource(fieldRef = Option(ObjectFieldSelector(fieldPath = "metadata.name")))
//                        )
//                      ),
//                      EnvVar(
//                        name = "ORKESTRA_NAMESPACE",
//                        valueFrom = Option(
//                          EnvVarSource(fieldRef = Option(ObjectFieldSelector(fieldPath = "metadata.namespace")))
//                        )
//                      )
//                    )
//                  )
//                )
//              )
//            )
//          )
//        )
//      )
//    )
//  )
//
//  def awaitorkestraReady(kubernetesClient: KubernetesClient): Future[Unit] =
//    kubernetesClient.services
//      .namespace(Kubernetes.namespace.metadata.get.name.get)
//      .proxy(service.metadata.get.name.get, HttpMethods.GET, "/api")
//      .void
//      .recoverWith {
//        case KubernetesException(StatusCodes.ServiceUnavailable.intValue, _, _) =>
//          Thread.sleep(1.second.toMillis)
//          awaitorkestraReady(kubernetesClient)
//        case KubernetesException(_, _, _) => Future.unit
//      }
//
//  def apply(kubernetesClient: KubernetesClient) =
//    for {
//      _ <- kubernetesClient.namespaces.createOrUpdate(Kubernetes.namespace)
//      _ <- kubernetesClient.services.namespace(Kubernetes.namespace.metadata.get.name.get).create(service)
//      _ <- kubernetesClient.deployments.namespace(Kubernetes.namespace.metadata.get.name.get).create(deployment)
//      _ <- awaitorkestraReady(kubernetesClient)
//    } yield ()
//}
