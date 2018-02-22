package io.chumps.orchestra.kubernetes

import com.goyeau.kubernetesclient.KubernetesClient

import io.chumps.orchestra.OrchestraConfig

object MasterPod {

  def get()(implicit orchestraConfig: OrchestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orchestraConfig.namespace).get(orchestraConfig.podName)
}
