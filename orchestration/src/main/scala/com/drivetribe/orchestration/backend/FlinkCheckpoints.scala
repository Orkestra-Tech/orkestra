package com.drivetribe.orchestration.backend

import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import shapeless._

object FlinkCheckpoints {

  def jobDefinition(environment: Environment) = Job[() => Unit](Symbol(s"flinkCheckpoints$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(PodConfig(AnsibleContainer :: HNil, Map("cronJob" -> "true")))(apply(environment) _)

  def board(environment: Environment) = JobBoard("Flink Checkpoints", jobDefinition(environment))

  private lazy val logger = Logger(getClass)

  def apply(environment: Environment)(ansible: AnsibleContainer.type)(): Unit = {
    Git.checkoutInfrastructure()

    dir("infrastructure") { implicit workDir =>
      ansible.install()
      saveCheckpoints(environment, ansible)
    }
  }

  def saveCheckpoints(environment: Environment, ansible: AnsibleContainer.type)(implicit workDir: Directory) =
    dir("ansible") { implicit workDir =>
      logger.info("Flink Checkpoints")
      ansible.playbook("flink-savepoint-manager.yml", s"-e env_name=${environment.entryName}")
    }
}
