package com.goyeau.orchestration

import java.io.File

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.sys.process._

import com.goyeau.orchestra._
import com.goyeau.orchestra.kubernetes._
import com.goyeau.orchestra.{Job, OrchestraConfig}
import com.goyeau.orchestra.kubernetes.Container
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
    val infrastructure = new File(OrchestraConfig.workspace, "infrastructure")
    Git
      .cloneRepository()
      .setURI(s"https://github.com/drivetribe/infrastructure.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider("drivetribeci", "12aeb93d19a941063619cd3d4765ef3f43ecd89d")
      )
      .setDirectory(infrastructure)
      .call()

    val exec = "ansible-galaxy install -r infrastructure/ansible/requirements.yml" !> ansible
    Await.ready(exec, Duration.Inf)

    println("Try to acquire lock on env")
    Lock.onEnvironment(environment) {
      println("Acquired lock on env")
    }

    println("Try to acquire lock on env again")
    Lock.onEnvironment(environment) {
      println("Acquired lock on env again")
    }
  }
}
