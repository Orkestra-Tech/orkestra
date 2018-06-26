package tech.orkestra.integration.tests.utils

import java.io.File
import java.util.UUID

import com.goyeau.kubernetes.client.{KubeConfig, KubernetesClient}
import io.k8s.api.core.v1.Namespace
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta

import tech.orkestra.{kubernetes, OrkestraConfig}
import tech.orkestra.utils.AkkaImplicits._

object Kubernetes {
  val namespace = Namespace(
    metadata = Option(ObjectMeta(name = Option(s"orkestra-test-${UUID.randomUUID().toString.takeWhile(_ != '-')}")))
  )

  val configFile = new File(s"${System.getProperty("user.home")}/.kube/config")
  implicit val orkestraConfig = OrkestraConfig.fromEnvVars()
  val client =
    if (configFile.exists()) KubernetesClient(KubeConfig(configFile, "minikube"))
    else kubernetes.Kubernetes.client
}
