package com.goyeau.orchestra.kubernetes

import java.io.File

import scala.io.Source

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.kubernetesclient.KubernetesClient
import com.goyeau.kubernetesclient.KubeConfig

object Kubernetes {

  lazy val client = KubernetesClient(
    KubeConfig(
      server = OrchestraConfig.kubeUri,
      oauthToken = Option(Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString),
      caCertFile = Option(new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    )
  )
}
