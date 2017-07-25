package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.Config

object MasterPod {

  def get() =
    Kubernetes.client.pods
      .inNamespace(Config.namespace)
      .withName(Config.podName)
      .get()
}
