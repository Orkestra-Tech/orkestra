package tech.orkestra.kubernetes

import java.io.File

import scala.io.Source

import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.OrkestraConfig
import com.goyeau.kubernetes.client.KubernetesClient
import com.goyeau.kubernetes.client.KubeConfig

object Kubernetes {

  def client(implicit orkestraConfig: OrkestraConfig) = KubernetesClient(
    KubeConfig(
      server = orkestraConfig.kubeUri,
      oauthToken = Option(Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString),
      caCertFile = Option(new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    )
  )
}
