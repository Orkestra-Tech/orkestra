package tech.orkestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await

import com.goyeau.kubernetes.client.KubernetesClient
import io.k8s.api.core.v1.Container

import tech.orkestra.OrkestraConfig
import tech.orkestra.filesystem.Directory
import tech.orkestra.kubernetes.Kubernetes

trait BlockingShells {
  protected def kubernetesClient: KubernetesClient

  val shellUtils = new Shells {
    override lazy val orkestraConfig = BlockingShells.orkestraConfig
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
  implicit private lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
}
