package com.drivetribe.orchestration.backend

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import com.drivetribe.orchestration.backend.DeployBackend.ansibleColourEnvParam
import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, Lock, Project, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.parameter.{Checkbox, Input, Select}
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import shapeless.{::, HNil}

object DeployFlinkJob {

  def jobDefinition(environment: Environment) =
    Job[EnvironmentSide :: String :: String :: Boolean :: HNil](Symbol(s"deployFlinkJob$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Deploy Flink job", jobDefinition(environment))(
      Select("Side", EnvironmentSide, Option(EnvironmentSide.Inactive)),
      Input[String]("Version"),
      Input[String]("Job name"),
      Checkbox("Kill existing job", checked = true)
    )

  private lazy val logger = Logger(getClass)

  def apply(environment: Environment)(
    ansible: AnsibleContainer.type
  )(side: EnvironmentSide, version: String, jobName: String, killExistingJob: Boolean): Unit = {
    Git.checkoutInfrastructure()

    Lock.onDeployment(environment, Project.Backend) {
      dir("infrastructure") { implicit workDir =>
        ansible.install()

        val colour = (environment.isBiColour, side) match {
          case (true, EnvironmentSide.Active)   => Some(Colour.getActive(environment))
          case (true, EnvironmentSide.Inactive) => Some(Colour.getActive(environment).opposite)
          case (true, EnvironmentSide.Common) =>
            throw new IllegalArgumentException(s"$side is not an applicable side to deploy REST API")
          case (false, _) => None
        }
        val stateVersion = Version(environment).state

        Await.result(
          deploy(environment, version, stateVersion, colour, ansible, jobName, killExistingJob),
          Duration.Inf
        )
      }
    }
  }

  def deploy(environment: Environment,
             version: String,
             stateVersion: String,
             colour: Option[EnvironmentColour],
             ansible: AnsibleContainer.type,
             jobName: String = "all-jobs",
             killExistingJob: Boolean = false)(implicit workDir: Directory) = Future {
    logger.info("Deploy Flink")
    dir("ansible") { implicit workDir =>
      val flinkJobEnvParam =
        if (environment.isProd && jobName == "all-jobs") s"flink_job_list=default"
        else s"single_flink_job=$jobName"

      val envParams = Seq(
        s"env_name=${environment.entryName}",
        s"data_processing_version=$version",
        s"dt_tools_version=$version",
        s"kill_existing_job=$killExistingJob",
        flinkJobEnvParam,
        StateVersions.template(stateVersion)
      ) ++ ansibleColourEnvParam(colour)

      ansible.playbook(
        "deploy-flink.yml",
        envParams.map(p => s"-e $p").mkString(" ")
      )
    }
  }
}
