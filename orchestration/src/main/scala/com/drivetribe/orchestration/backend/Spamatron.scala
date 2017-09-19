package com.drivetribe.orchestration.backend

import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Git, _}
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.PodConfig
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import shapeless._

object Spamatron {

  lazy val jobDefinition = Job[() => Unit]('spamatron)

  lazy val job = jobDefinition(PodConfig(AnsibleContainer :: HNil))(apply _)

  lazy val board = JobBoard("Spamatron", jobDefinition)

  private lazy val logger = Logger(getClass)

  def apply(ansible: AnsibleContainer.type)(): Unit = {
    Git.checkoutInfrastructure()

    dir("infrastructure") { implicit workDir =>
      ansible.install()
      spamatron(ansible)
    }
  }

  def spamatron(ansible: AnsibleContainer.type)(implicit workDir: Directory) =
    stage("Spamatron") {
      dir("ansible") { implicit workDir =>
        val environment = Environment.Prod
        val version = Version(environment)

        val params = Seq(
          s"env_name=${environment.entryName}",
          s"colour=${Colour.getActive(environment).entryName}",
          s"dt_tools_version=${version.build}",
          StateVersions.template(version.state)
        )

        ansible.playbook("email-tool.yml", params.map(p => s"-e $p").mkString(" "))
      }
    }
}
