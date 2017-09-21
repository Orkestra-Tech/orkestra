package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.OrchestraConfig

object MasterPod {

  def get() =
    Kubernetes.client.pods
      .namespace(OrchestraConfig.namespace)(OrchestraConfig.podName)
      .get()
}
