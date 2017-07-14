package com.goyeau.orchestration

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.goyeau.orchestra._
import com.goyeau.orchestra.kubernetes._
import com.goyeau.orchestra.Job
import com.goyeau.orchestra.io.LocalFile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object CreateEnvironment {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"create${environment.entryName}"))

  def job(environment: Environment) =
    jobDefinition(environment)(
      PodConfig(
        Container("ansible", "registry.drivetribe.com/tools/ansible:cached", tty = true, Seq("cat")),
        Container("terraform", "hashicorp/terraform:0.9.8", tty = true, Seq("cat"))
      )
    )(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Create", jobDefinition(environment))(Param[String]("sourceEnv", defaultValue = Some("staging")))

  def apply(environment: Environment)(ansible: Container, terraform: Container)(sourceEnv: String) = {
    Git
      .cloneRepository()
      .setURI(s"https://github.com/drivetribe/infrastructure.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_TOKEN"))
      )
      .setDirectory(LocalFile("infrastructure"))
      .call()

    Lock.onEnvironment(environment) {
      dir("infrastructure") { implicit wd =>
        println("Install Ansible deps")
        val ansibleDeps = "ansible-galaxy install -r ansible/requirements.yml" !> ansible
        Await.ready(ansibleDeps, Duration.Inf)

        dir(s"terraform/providers/aws/app/${environment.environmentType.entryName}") { implicit wd =>
          println("Init Terraform")
          val terraformInit = s"terraform init -backend-config=key=tfstates/app-${environment.entryName}.tfstate" !> terraform
          Await.ready(terraformInit, Duration.Inf)

          println("Init Ansible")
          val ansibleInit = s"ansible-playbook init.yml --vault-password-file /opt/docker/secrets/ansible/vault-pass --private-key /opt/docker/secrets/ssh-key.pem -e env_name=${environment.entryName}" !> ansible
          Await.ready(ansibleInit, Duration.Inf)
        }
      }
    }

    println("Try to acquire lock on env again")
    Lock.onEnvironment(environment) {
      println("Acquired lock on env again")
    }
  }
}
