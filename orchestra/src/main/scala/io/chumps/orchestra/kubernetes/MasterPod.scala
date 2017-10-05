package io.chumps.orchestra.kubernetes

import io.chumps.orchestra.AkkaImplicits._
import io.chumps.orchestra.OrchestraConfig

object MasterPod {

  def get() =
    Kubernetes.client.pods
      .namespace(OrchestraConfig.namespace)(OrchestraConfig.podName)
      .get()
}
