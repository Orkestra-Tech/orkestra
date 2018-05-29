package com.goyeau.orchestra.kubernetes

import com.goyeau.kubernetesclient.KubernetesClient

import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.orchestra.utils.AkkaImplicits._

private[orchestra] object MasterPod {

  def get()(implicit orchestraConfig: OrchestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orchestraConfig.namespace).get(orchestraConfig.podName)
}
