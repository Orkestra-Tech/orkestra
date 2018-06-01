package com.goyeau.orkestra.kubernetes

import java.io.File

import scala.io.Source

import com.goyeau.orkestra.utils.AkkaImplicits._
import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.kubernetesclient.KubernetesClient
import com.goyeau.kubernetesclient.KubeConfig

object Kubernetes {

  def client(implicit orkestraConfig: OrkestraConfig) = KubernetesClient(
    KubeConfig(
      server = orkestraConfig.kubeUri,
      oauthToken = Option(Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString),
      caCertFile = Option(new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    )
  )
}
