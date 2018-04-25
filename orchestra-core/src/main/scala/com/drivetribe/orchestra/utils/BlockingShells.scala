package com.drivetribe.orchestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await

import com.goyeau.kubernetesclient.KubernetesClient
import io.k8s.api.core.v1.Container

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.filesystem.Directory
import com.drivetribe.orchestra.kubernetes.Kubernetes

trait BlockingShells {
  protected def kubernetesClient: KubernetesClient

  val shellUtils = new Shells {
    override lazy val orchestraConfig = BlockingShells.orchestraConfig
    override lazy val kubernetesClient = BlockingShells.kubernetesClient
  }

  /**
    * Run a shell script in the work directory passed in the implicit workDir.
    * This is a blocking call.
    */
  def sh(script: String)(implicit workDir: Directory): String =
    Await.result(shellUtils.sh(script), Duration.Inf)

  /**
    * Run a shell script in the given container and in the work directory passed in the implicit workDir.
    * This is a blocking call.
    */
  def sh(script: String, container: Container)(implicit workDir: Directory): String =
    Await.result(shellUtils.sh(script, container), Duration.Inf)
}

object BlockingShells extends BlockingShells {
  implicit private lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
}
