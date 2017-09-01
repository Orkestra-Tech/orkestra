package com.drivetribe.orchestration.infrastructure

import java.io.{File, IOException}

import com.drivetribe.orchestration.{Environment, Git, Lock}
import com.goyeau.kubernetesclient.{KubeConfig, KubernetesClient}
import com.goyeau.orchestra.{Job, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.typesafe.scalalogging.Logger
import com.goyeau.orchestra.AkkaImplicits._

object DestroyEnvironment {

  def jobDefinition(environment: Environment) = Job[() => Unit](Symbol(s"destroy$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) = SingleJobBoard("Destroy", jobDefinition(environment))

  private lazy val logger = Logger(getClass)

  def apply(environment: Environment)(ansible: AnsibleContainer.type, terraform: TerraformContainer.type)(): Unit = {
    Git.checkoutInfrastructure()

    Lock.onEnvironment(environment) {
      dir("infrastructure") { implicit workDir =>
        // @TODO Remove this hack when federation can delete a namespaces
        cleanKubernetes(environment)
        ansible.install()
        Init(environment, ansible, terraform)
        destroy(environment, terraform)
      }
    }
  }

  def cleanKubernetes(environment: Environment) = {
    logger.info("Clean Kubernetes")
    val kube = KubernetesClient(KubeConfig(new File("/opt/docker/secrets/kube/config")))
    val deleteAll = for {
      _ <- kube.namespaces(environment.entryName).services.delete()
      _ <- kube.namespaces(environment.entryName).deployments.delete()
    } yield ()
    deleteAll.failed.foreach {
      case e: IOException if e.getMessage.contains("""namespaces \"nardo\" not found""") =>
      case e                                                                             => throw e
    }
  }

  def destroy(environment: Environment, terraform: TerraformContainer.type)(implicit workDir: Directory) = {
    logger.info("Destroying")
    // Remove prevent_destroy security
    sh("find terraform -type f -name '*.tf' -exec sed -i 's/prevent_destroy *= .*/prevent_destroy = false/g' {} +")
    dir(terraform.rootDir(environment)) { implicit workDir =>
      terraform.destroy()
    }
  }
}
