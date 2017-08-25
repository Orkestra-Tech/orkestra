package com.drivetribe.orchestration.infrastructure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.drivetribe.orchestration._
import com.drivetribe.orchestration.backend.{DeployBackend, SqlCopy}
import com.drivetribe.orchestration.frontend.DeployFrontend
import com.drivetribe.orchestration.{Git, Lock}
import com.goyeau.orchestra.{Job, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.typesafe.scalalogging.Logger

object CreateEnvironment {

  def jobDefinition(environment: Environment) =
    Job[(Environment, Boolean, Boolean) => Unit](Symbol(s"create$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Create", jobDefinition(environment))(
      EnumParam("Source Environment", Environment, defaultValue = Some(Environment.Staging)),
      Param[Boolean]("Deploy Frontend", defaultValue = Some(true)),
      Param[Boolean]("Deploy Backend")
    )

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(
    ansible: AnsibleContainer.type,
    terraform: TerraformContainer.type
  )(sourceEnv: Environment, deployFrontend: Boolean, deployBackend: Boolean): Unit = {
    Git.checkoutInfrastructure()

    Lock.onEnvironment(environment) {
      dir("infrastructure") { implicit workDir =>
        ansible.install()
        Init(environment, ansible, terraform)
        Seq(
          provisionCloudResources(environment, terraform),
          provisionKafkaZookeeper(environment, sourceEnv, ansible),
          provisionElasticsearch(environment, ansible)
        ).parallel
      }
    }

    Seq(
      Future(SqlCopy.job.trigger(Environment.Staging.entryName, environment.entryName)),
      Future(if (deployFrontend) DeployFrontend.job(environment).trigger(frontend.Version(Environment.Staging))),
      Future(if (deployBackend) DeployBackend.job(environment).trigger(backend.Version(Environment.Staging)))
    ).parallel
  }

  def provisionCloudResources(environment: Environment,
                              terraform: TerraformContainer.type)(implicit workDir: Directory) = Future {
    dir(terraform.rootDir(environment)) { implicit workDir =>
      println("Provision cloud resources")
      val elasticsearchModules =
        if (environment.environmentType == EnvironmentType.Large)
          Seq("module.elasticsearch_black", "module.elasticsearch_white")
        else Seq("module.elasticsearch")

      val targets = Seq(
        "module.kafka_zookeeper",
        "data.terraform_remote_state.vpc", // @TODO to remove hacky bug fix
        "data.terraform_remote_state.kafka_mirror" // @TODO to remove hacky fix
      ) ++ elasticsearchModules

      terraform.apply(s"-var bootstrap_git_branch=master ${targets.map(t => s"-target=$t").mkString(" ")}")
      terraform.apply(
        s"-var ansible_key=${System.getenv("ANSIBLE_VAULT_PASS")} -var bootstrap_git_branch=master"
      )
    }
  }

  // @TODO Replace sleep either by bootstraping themself or checking that the hardware is there
  val cloudProvisiongTime = 280000

  def provisionKafkaZookeeper(environment: Environment, sourceEnv: Environment, ansible: AnsibleContainer.type)(
    implicit workDir: Directory
  ) = Future {
    dir("ansible") { implicit workDir =>
      Thread.sleep(cloudProvisiongTime)
      println("Provision Kafka and Zookeeper")
      ansible
        .playbook("kafka-zookeeper.yml", s"-e env_name=${environment.entryName} -e from_env=${sourceEnv.entryName}")
    }
  }

  def provisionElasticsearch(environment: Environment, ansible: AnsibleContainer.type)(implicit workDir: Directory) =
    Future {
      dir("ansible") { implicit workDir =>
        Thread.sleep(cloudProvisiongTime)
        println("Provision Elasticsearch")
        ansible.playbook("elasticsearch.yml", s"-e env_name=${environment.entryName}")
      }
    }
}
