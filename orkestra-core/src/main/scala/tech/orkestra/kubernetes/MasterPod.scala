package tech.orkestra.kubernetes

import com.goyeau.kubernetes.client.KubernetesClient

import tech.orkestra.OrkestraConfig
import tech.orkestra.utils.AkkaImplicits._

private[orkestra] object MasterPod {

  def get()(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient) =
    kubernetesClient.pods.namespace(orkestraConfig.namespace).get(orkestraConfig.podName)
}
