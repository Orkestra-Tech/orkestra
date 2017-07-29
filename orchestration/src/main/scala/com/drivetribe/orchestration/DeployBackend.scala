package com.drivetribe.orchestration

import scala.concurrent.Future

import com.goyeau.orchestra.{Job, _}
import com.goyeau.orchestra.filesystem.Directory
import com.typesafe.scalalogging.Logger
import scala.concurrent.ExecutionContext.Implicits.global

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
        ansible.install
        Init(environment, ansible, terraform)

        val activeColour =
          if (environment.isBiColour) Some(BiColour.getActiveColour(environment))
          else None
        val inactiveColour = activeColour.map(_.opposite)

        dtTools(environment, version, inactiveColour, ansible)
        Seq(
          deployRestApi(environment, version, inactiveColour, terraform),
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

  def deployRestApi(environment: Environment,
                    version: String,
                    colour: Option[EnvironmentColour],
                    terraform: TerraformContainer.type)(implicit workDir: Directory) = Future {
    println("Deploy REST API")
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
