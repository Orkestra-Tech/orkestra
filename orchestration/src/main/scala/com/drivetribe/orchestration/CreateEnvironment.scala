package com.drivetribe.orchestration

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.goyeau.orchestra._
import com.goyeau.orchestra.Job
import com.goyeau.orchestra.io.{Directory, LocalFile}
import com.typesafe.scalalogging.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object CreateEnvironment {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"create${environment.entryName}"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Create", jobDefinition(environment))(Param[String]("sourceEnv", defaultValue = Some("staging")))

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(ansible: AnsibleContainer.type,
                                      terraform: TerraformContainer.type)(sourceEnv: String): Unit = {
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
      dir("infrastructure") { implicit workDir =>
        ansible.install
        Init(environment, ansible, terraform)
        Seq(
          provisionCloudResources(environment, terraform),
          provisionKafkaZookeeper(environment, Environment.withNameInsensitive(sourceEnv), ansible),
          provisionElasticsearch(environment, ansible)
        ).parallel
      }
    }
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

      terraform.apply(s"-var bootstrap_git_branch=master${targets.map(t => s" -target=$t").mkString}")
      terraform.apply(
        s"""-var ansible_key="${System.getenv("ANSIBLE_VAULT_PASS")}" -var bootstrap_git_branch=master"""
      )
    }
  }

  // @TODO Replace sleep either by bootstraping themself or checking that the hardware is there
  val cloudProvisiongTime = 270000

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
