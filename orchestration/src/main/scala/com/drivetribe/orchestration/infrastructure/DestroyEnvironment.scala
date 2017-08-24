package com.drivetribe.orchestration.infrastructure

import java.io.File

import com.drivetribe.orchestration.{Git, Lock}
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

  lazy val logger = Logger(getClass)

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
    println("Clean Kubernetes")
    val kube = KubernetesClient(KubeConfig(new File("/opt/docker/secrets/kube/config")))
    kube.namespaces(environment.entryName).services.delete()
    kube.namespaces(environment.entryName).deployments.delete()
  }

  def destroy(environment: Environment, terraform: TerraformContainer.type)(implicit workDir: Directory) = {
    println("Destroying")
    // Remove prevent_destroy security
    sh("find terraform -type f -name '*.tf' -exec sed -i 's/prevent_destroy *= .*/prevent_destroy = false/g' {} +")
    dir(terraform.rootDir(environment)) { implicit workDir =>
      terraform.destroy()
    }
  }
}
