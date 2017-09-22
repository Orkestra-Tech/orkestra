package com.drivetribe.orchestration.backend

import com.drivetribe.orchestration.{Environment, Git}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.parameter.Input
import com.goyeau.orchestra.{Job, _}
import io.k8s.api.core.v1.{HostPathVolumeSource, Volume}
import shapeless._

object BuildAndDeployBackend {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"buildAndDeployBackend$environment"))

  def job(environment: Environment) =
    jobDefinition(environment)(
      PodConfig(
        SbtContainer :: HNil,
        volumes = Seq(Volume("docker-sock", hostPath = Option(HostPathVolumeSource("/var/run/docker.sock"))))
      )
    )(apply(environment) _)

  def board(environment: Environment) =
    JobBoard("Build and Deploy Backend", jobDefinition(environment))(Input[String]("Branch"))

  def apply(environment: Environment)(sbt: SbtContainer.type)(branch: String): Unit = {
    Git.checkoutBackend(branch)

    val version = dir("backend") { implicit workDir =>
      publish(sbt)
    }

    DeployBackend.job(environment).run(version)
  }

  def publish(sbt: SbtContainer.type)(implicit workDir: Directory): String =
    stage("Publish") {
      sbt("rest-api/docker:publish data-processing/publish drivetribe-tools/docker:publish")
      sbt("-Dsbt.log.noformat=true version").trim.split(" ").last
    }
}
