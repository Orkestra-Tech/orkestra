package com.drivetribe.orchestra.kubernetes

import com.goyeau.kubernetesclient.KubernetesClient

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.utils.AkkaImplicits._

private[orchestra] object MasterPod {

  def get()(implicit orchestraConfig: OrchestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orchestraConfig.namespace).get(orchestraConfig.podName)
}
