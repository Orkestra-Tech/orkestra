package com.drivetribe.orchestration.infrastructure

import com.drivetribe.orchestration.Environment
import com.goyeau.orchestra._
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.Container
import com.typesafe.scalalogging.LazyLogging

object TerraformContainer
    extends Container("terraform", "hashicorp/terraform:0.9.8", tty = true, Seq("cat"))
    with LazyLogging {

  def rootDir(environment: Environment) = s"terraform/providers/aws/app/${environment.environmentType.entryName}"

  def init(remoteState: String)(implicit workDir: Directory) = {
    logger.info("Init Terraform")
    sh(s"terraform init -backend-config=key=tfstates/$remoteState.tfstate", this)
  }

  def apply(params: String = "")(implicit workDir: Directory) =
    sh(s"terraform apply $params", this)

  def destroy(params: String = "")(implicit workDir: Directory) =
    sh(s"terraform destroy -force $params", this)
}
