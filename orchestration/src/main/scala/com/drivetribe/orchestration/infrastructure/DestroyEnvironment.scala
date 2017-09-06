package com.drivetribe.orchestration.infrastructure

import com.drivetribe.orchestration.{Environment, Git, Lock}
import com.goyeau.orchestra.{Job, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.typesafe.scalalogging.Logger

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
        ansible.install()
        Init(environment, ansible, terraform)
        destroy(environment, terraform)
      }
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
