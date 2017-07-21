package com.drivetribe.orchestration

import com.goyeau.orchestra._
import com.goyeau.orchestra.io.{Directory, LocalFile}
import com.goyeau.orchestra.kubernetes._
import com.goyeau.orchestra.Job
import com.typesafe.scalalogging.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object DestroyEnvironment {

  def jobDefinition(environment: Environment) = Job[() => Unit](Symbol(s"destroy${environment.entryName}"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) = SingleJobBoard("Destroy", jobDefinition(environment))

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(ansible: AnsibleContainer.type, terraform: TerraformContainer.type)(): Unit = {
    val git = Git
      .cloneRepository()
      .setURI(s"https://github.com/drivetribe/infrastructure.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_TOKEN"))
      )
      .setDirectory(LocalFile("infrastructure"))
      .setNoCheckout(true)
      .call()
    git.getRepository.getConfig.setBoolean("core", null, "fileMode", true)
    git.checkout().setName("origin/master").call()

    Lock.onEnvironment(environment) {
      dir(s"infrastructure") { implicit workDir =>
        // @TODO Clean Kubernetes
        ansible.install
        Init(environment, ansible, terraform)
        destroy(environment, terraform)
      }
    }
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
