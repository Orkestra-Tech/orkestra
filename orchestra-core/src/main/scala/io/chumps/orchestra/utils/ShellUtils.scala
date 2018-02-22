package io.chumps.orchestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await

import com.goyeau.kubernetesclient.KubernetesClient
import io.k8s.api.core.v1.Container

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.filesystem.Directory
import io.chumps.orchestra.kubernetes.Kubernetes

trait ShellUtils {
  protected val orchestraConfig: OrchestraConfig
  protected val kubernetesClient: KubernetesClient

  val asyncShellUtils = new AsyncShellUtils {
    override val orchestraConfig = ShellUtils.orchestraConfig
    override val kubernetesClient = ShellUtils.kubernetesClient
  }

  def sh(script: String)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script), Duration.Inf)

  def sh(script: String, container: Container)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script, container), Duration.Inf)
}

object ShellUtils extends ShellUtils {
  override implicit val orchestraConfig = OrchestraConfig.fromEnvVars()
  override val kubernetesClient = Kubernetes.client
}
