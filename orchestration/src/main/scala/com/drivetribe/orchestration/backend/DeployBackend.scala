package com.drivetribe.orchestration.backend

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import com.drivetribe.orchestration._
import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, Lock, Project}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.parameter.Input
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import shapeless._

object DeployBackend {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"deployBackend$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(HList(AnsibleContainer, TerraformContainer)))(apply(environment) _)

  def board(environment: Environment) =
    JobBoard("Deploy Backend", jobDefinition(environment))(Input[String]("Version"))

  private lazy val logger = Logger(getClass)

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
        val stateVersion = Instant.now().getEpochSecond.toString

        dtTools(environment, version, stateVersion, inactiveColour, ansible)
        Seq(
          DeployRestApi.deploy(environment, version, stateVersion, inactiveColour, terraform, ansible),
          DeployFlinkJob.deploy(environment, version, stateVersion, inactiveColour, ansible)
        ).parallel
      }
    }
  }

  def dtTools(environment: Environment,
              version: String,
              stateVersion: String,
              colour: Option[EnvironmentColour],
              ansible: AnsibleContainer.type)(implicit workDir: Directory) = {
    logger.info("DT Tools")
    dir("ansible") { implicit workDir =>
      val params = Seq(
        s"env_name=${environment.entryName}",
        s"dt_tools_version=$version",
        StateVersions.template(stateVersion)
      ) ++ ansibleColourEnvParam(colour)

      ansible.playbook("dt-tools.yml", params.map(p => s"-e $p").mkString(" "))
    }
  }

  def ansibleColourEnvParam(colour: Option[EnvironmentColour]) = colour.map(c => s"colour=${c.entryName}")
}
