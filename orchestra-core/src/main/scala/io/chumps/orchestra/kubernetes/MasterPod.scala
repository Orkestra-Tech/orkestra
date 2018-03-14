package io.chumps.orchestra.kubernetes

import com.goyeau.kubernetesclient.KubernetesClient

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.utils.AkkaImplicits._

object MasterPod {

  def get()(implicit orchestraConfig: OrchestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orchestraConfig.namespace).get(orchestraConfig.podName)
}
