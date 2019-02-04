package tech.orkestra.kubernetes

import com.goyeau.kubernetes.client.KubernetesClient
import io.k8s.api.core.v1.Pod
import tech.orkestra.OrkestraConfig

private[orkestra] object MasterPod {

  def get[F[_]](implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[Pod] =
    kubernetesClient.pods.namespace(orkestraConfig.namespace).get(orkestraConfig.podName)
}
