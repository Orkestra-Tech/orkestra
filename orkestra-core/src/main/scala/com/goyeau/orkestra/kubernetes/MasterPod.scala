package com.goyeau.orkestra.kubernetes

import com.goyeau.kubernetesclient.KubernetesClient

import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.utils.AkkaImplicits._

private[orkestra] object MasterPod {

  def get()(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orkestraConfig.namespace).get(orkestraConfig.podName)
}
