package com.drivetribe.orchestration.backend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, Lock, Project, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.parameter.{EnumParam, Input}
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger

object DeployRestApi {

  def jobDefinition(environment: Environment) =
    Job[(EnvironmentSide, String) => Unit](Symbol(s"deployRestApi$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Deploy REST API", jobDefinition(environment))(
      EnumParam("Side", EnvironmentSide, Option(EnvironmentSide.Inactive)),
      Input[String]("Version")
    )

  private lazy val logger = Logger(getClass)

  def apply(environment: Environment)(
    ansible: AnsibleContainer.type,
    terraform: TerraformContainer.type
  )(side: EnvironmentSide, version: String): Unit = {
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

        Await.result(deploy(environment, version, colour, terraform, ansible), Duration.Inf)
      }
    }
  }

  def deploy(environment: Environment,
             version: String,
             colour: Option[EnvironmentColour],
             terraform: TerraformContainer.type,
             ansible: AnsibleContainer.type)(implicit workDir: Directory) = Future {
    logger.info("Deploy REST API")
    Init(environment, ansible, terraform)

    dir(terraform.rootDir(environment)) { implicit workDir =>
      val moduleName = colour.fold("rest_api")(c => s"rest_api_$c")
      val stateVersions = StateVersions.template(version).replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}")
      val tfState = TerraformState.fromS3(environment)
      val capacity = AutoScaling.getDesiredCapacity(
        tfState.getResourceAttribute(Seq("root", moduleName), "aws_autoscaling_group.api", "name")
      )

      val params = Seq(
        s"-target=module.$moduleName",
        s"-target=data.terraform_remote_state.vpc", // @TODO to remove hacky bug fix
        s"-var ansible_key=${System.getenv("ANSIBLE_VAULT_PASS")}",
        s"-var api_version=$version",
        s"-var state_versions=$stateVersions",
        s"-var bootstrap_git_branch=master",
        s"-var api_desired_instances_count=$capacity"
      ) ++ colour.map(c => s"-var active_colour=${c.entryName}")

      terraform.apply(params.mkString(" "))
    }
  }
}
