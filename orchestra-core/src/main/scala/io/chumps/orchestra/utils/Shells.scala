package io.chumps.orchestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await

import com.goyeau.kubernetesclient.KubernetesClient
import io.k8s.api.core.v1.Container

import io.chumps.orchestra.OrchestraConfig
import io.chumps.orchestra.filesystem.Directory
import io.chumps.orchestra.kubernetes.Kubernetes

trait Shells {
  protected val orchestraConfig: OrchestraConfig
  protected val kubernetesClient: KubernetesClient

  val asyncShellUtils = new AsyncShells {
    override val orchestraConfig = Shells.orchestraConfig
    override val kubernetesClient = Shells.kubernetesClient
  }

  def sh(script: String)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script), Duration.Inf)

  def sh(script: String, container: Container)(implicit workDir: Directory): String =
    Await.result(asyncShellUtils.sh(script, container), Duration.Inf)
}

object Shells extends Shells {
  override implicit val orchestraConfig = OrchestraConfig.fromEnvVars()
  override val kubernetesClient = Kubernetes.client
}
