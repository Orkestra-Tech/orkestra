package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.OrchestraConfig

object MasterPod {

  def get() =
    Kubernetes.client
      .namespaces(OrchestraConfig.namespace)
      .pods(OrchestraConfig.podName)
      .get()
}
