package io.chumps.orchestra.integration.tests.utils

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import com.goyeau.kubernetesclient.{IntValue, KubernetesClient, KubernetesException}
import io.k8s.api.apps.v1beta2.{Deployment, DeploymentSpec}
import io.k8s.api.core.v1._
import io.k8s.apimachinery.pkg.apis.meta.v1.{LabelSelector, ObjectMeta}

import io.chumps.orchestra.integration.tests.BuildInfo
import io.chumps.orchestra.utils.AkkaImplicits._

object DeployOrchestration {
  val appOrchestrationLabel = Option(Map("app" -> "orchestration"))

  val service = Service(
    metadata = Option(ObjectMeta(name = Option("orchestration"))),
    spec = Option(
      ServiceSpec(
        selector = appOrchestrationLabel,
        ports = Option(Seq(ServicePort(port = 80, targetPort = Option(IntValue(80)))))
      )
    )
  )

  val deployment = Deployment(
    metadata = Option(ObjectMeta(name = Option("orchestation"))),
    spec = Option(
      DeploymentSpec(
        replicas = Option(1),
        selector = Option(LabelSelector(matchLabels = appOrchestrationLabel)),
        template = PodTemplateSpec(
          metadata = Option(ObjectMeta(labels = appOrchestrationLabel)),
          spec = Option(
            PodSpec(
              containers = Seq(
                Container(
                  name = "orchestration",
                  image = Option(s"registry.drivetribe.com/tools/${BuildInfo.artifactName}:${BuildInfo.version}"),
                  imagePullPolicy = Option("IfNotPresent"),
                  env = Option(
                    Seq(
                      EnvVar(name = "ORCHESTRA_KUBE_URI", value = Option("https://kubernetes.default")),
                      EnvVar(name = "ORCHESTRA_ELASTICSEARCH_URI",
                             value = Option("elasticsearch://elasticsearch:9200")),
                      EnvVar(name = "ORCHESTRA_POD_NAME",
                             valueFrom = Option(
                               EnvVarSource(fieldRef = Option(ObjectFieldSelector(fieldPath = "metadata.name")))
                             )),
                      EnvVar(name = "ORCHESTRA_NAMESPACE",
                             valueFrom = Option(
                               EnvVarSource(fieldRef = Option(ObjectFieldSelector(fieldPath = "metadata.namespace")))
                             ))
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  def awaitOrchestrationReady(kubernetesClient: KubernetesClient): Future[Unit] =
    kubernetesClient.services
      .namespace(Kubernetes.namespace.metadata.get.name.get)
      .proxy(service.metadata.get.name.get, HttpMethods.GET, "api")
      .map(_ => ())
      .recoverWith {
        case KubernetesException(StatusCodes.ServiceUnavailable.intValue, _, _) =>
          Thread.sleep(1.second.toMillis)
          awaitOrchestrationReady(kubernetesClient)
        case KubernetesException(_, _, _) => Future.unit
      }

  def apply(kubernetesClient: KubernetesClient) =
    for {
      _ <- kubernetesClient.namespaces.createOrUpdate(Kubernetes.namespace)
      _ <- kubernetesClient.services.namespace(Kubernetes.namespace.metadata.get.name.get).create(service)
      _ <- kubernetesClient.deployments.namespace(Kubernetes.namespace.metadata.get.name.get).create(deployment)
      _ <- awaitOrchestrationReady(kubernetesClient)
    } yield ()
}
