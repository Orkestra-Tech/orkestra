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
    jobDefinition(environment)(PodConfig(AnsibleContainer, TerraformContainer))(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Create", jobDefinition(environment))(Param[String]("sourceEnv", defaultValue = Some("staging")))

  def apply(environment: Environment)(ansible: AnsibleContainer.type,
                                      terraform: TerraformContainer.type)(sourceEnv: String): Unit = {
    Git
      .cloneRepository()
      .setURI(s"https://github.com/drivetribe/infrastructure.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_TOKEN"))
      )
      .setDirectory(LocalFile("infrastructure"))
      .call()
    println()

    Lock.onEnvironment(environment) {
      dir("infrastructure") { implicit wd =>
        val ansibleDeps = ansible.install
        Await.ready(ansibleDeps, Duration.Inf)

        val run = dir(s"terraform/providers/aws/app/${environment.environmentType.entryName}") { implicit wd =>
          for {
            _ <- terraform.init(environment)
            _ <- ansible.init(environment)
            _ <- provisionKafkaZkElasticsearch(environment, terraform)
          } yield ()
        }
        Await.ready(run, Duration.Inf)
      }
    }
  }

  def provisionKafkaZkElasticsearch(environment: Environment, terraform: TerraformContainer.type) = {
    println("Provision Kafka, Zookeeper and Elasticsearch")
    val elasticsearchModules =
      if (environment.environmentType == EnvironmentType.Large)
        "-target=module.elasticsearch_black -target=module.elasticsearch_white"
      else "-target=module.elasticsearch"
    val targets = Seq(
      "module.kafka_zookeeper",
      "data.terraform_remote_state.vpc", // @TODO to remove hacky bug fix
      "data.terraform_remote_state.kafka_mirror" // @TODO to remove hacky fix
    )
    terraform.apply(s"$elasticsearchModules -var bootstrap_git_branch=master -target=${targets.mkString(" -target=")}")
  }

}
