package com.goyeau.orchestration

import java.io.File
import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.goyeau.orchestra._
import com.goyeau.orchestra.kubernetes._
import io.circe.generic.auto._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import scala.sys.process._

object Orchestration extends Orchestra {

  lazy val emptyTaskDef = Job[() => Unit]('emptyJob)
  lazy val emptyTask = emptyTaskDef(() => println("empty"))

  lazy val deployBackendDef = Job[(String, UUID) => Unit]('deployBackend)
  lazy val deployBackend = deployBackendDef((version, runId) => println(version + runId))

  lazy val createNardoDef = Job[String => Unit]('createNarado)
  lazy val createNardo =
    createNardoDef(
      PodConfig(
        Container("ansible", "registry.drivetribe.com/tools/ansible:cached", tty = true, Seq("cat")),
        Container("terraform", "hashicorp/terraform:0.9.8", tty = true, Seq("cat"))
      )
    )(createEnv("nardo"))

  def createEnv(environment: String)(ansible: Container, terraform: Container)(sourceEnv: String) = {
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

    println(sourceEnv)
  }

  lazy val jobs = Seq(
    emptyTask,
    createNardo,
    deployBackend
  )

  lazy val board = FolderBoard("Drivetribe")(
    FolderBoard("Operation")(
      FolderBoard("Staging")(
        SingleJobBoard("DeployBackend", deployBackendDef)(Param[String]("version", defaultValue = Some("12")), RunId)
      )
    ),
    FolderBoard("Infrastructure")(
      FolderBoard("Nardo")(
        SingleJobBoard("Create", createNardoDef)(Param[String]("sourceEnv", defaultValue = Some("staging")))
      )
    )
  )
}
