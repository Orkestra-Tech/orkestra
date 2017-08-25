package com.drivetribe.orchestration.backend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.drivetribe.orchestration._
import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, Lock, Project}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger

object DeployBackend {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"deployBackend$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Deploy Backend", jobDefinition(environment))(Param[String]("Version"))

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(ansible: AnsibleContainer.type,
                                      terraform: TerraformContainer.type)(version: String): Unit = {
    Git.checkoutInfrastructure()

    Lock.onDeployment(environment, Project.Backend) {
      dir("infrastructure") { implicit workDir =>
        ansible.install()

        val activeColour =
          if (environment.isBiColour) Some(Colour.getActive(environment))
          else None
        val inactiveColour = activeColour.map(_.opposite)

        dtTools(environment, version, inactiveColour, ansible)
        Seq(
          DeployRestApi.deploy(environment, version, inactiveColour, terraform, ansible),
          deployFlink(environment, version, inactiveColour, ansible)
        ).parallel
      }
    }
  }

  def dtTools(environment: Environment,
              version: String,
              colour: Option[EnvironmentColour],
              ansible: AnsibleContainer.type)(implicit workDir: Directory) = {
    println("DT Tools")
    dir("ansible") { implicit workDir =>
      val envParams = Seq(
        s"env_name=${environment.entryName}",
        s"dt_tools_version=$version",
        StateVersions.template(version)
      ) ++ ansibleColourEnvParam(colour)

      ansible.playbook(
        "dt-tools.yml",
        envParams.map(p => s"-e $p").mkString(" ")
      )
    }
  }

  def deployFlink(environment: Environment,
                  version: String,
                  colour: Option[EnvironmentColour],
                  ansible: AnsibleContainer.type)(
    implicit workDir: Directory
  ) = Future {
    println("Deploy Flink")
    dir("ansible") { implicit workDir =>
      val flinkJobEnvParam =
        if (environment.isProd) s"flink_job_list=default"
        else "single_flink_job=all-jobs"

      val envParams = Seq(
        s"env_name=${environment.entryName}",
        s"data_processing_version=$version",
        s"dt_tools_version=$version",
        flinkJobEnvParam,
        StateVersions.template(version)
      ) ++ ansibleColourEnvParam(colour)

      ansible.playbook(
        "deploy-flink.yml",
        envParams.map(p => s"-e $p").mkString(" ")
      )
    }
  }

  private def ansibleColourEnvParam(colour: Option[EnvironmentColour]) = colour.map(c => s"colour=${c.entryName}")
}
