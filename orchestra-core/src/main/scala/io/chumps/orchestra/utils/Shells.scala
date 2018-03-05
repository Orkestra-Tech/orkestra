package io.chumps.orchestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await

import com.goyeau.kubernetesclient.KubernetesClient
import io.k8s.api.core.v1.Container

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.filesystem.Directory
import io.chumps.orchestra.kubernetes.Kubernetes

trait Shells {
  protected def kubernetesClient: KubernetesClient

  val asyncShellUtils = new AsyncShells {
    override lazy val orchestraConfig = Shells.orchestraConfig
    override lazy val kubernetesClient = Shells.kubernetesClient
  }

  def sh(script: String)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script), Duration.Inf)

  def sh(script: String, container: Container)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script, container), Duration.Inf)
}

object Shells extends Shells {
  private implicit lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
}
