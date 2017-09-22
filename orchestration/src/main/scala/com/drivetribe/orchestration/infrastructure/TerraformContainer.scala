package com.drivetribe.orchestration.infrastructure

import com.drivetribe.orchestration.Environment
import com.goyeau.orchestra._
import com.goyeau.orchestra.filesystem.Directory
import com.typesafe.scalalogging.LazyLogging
import io.k8s.api.core.v1.Container

object TerraformContainer
    extends Container(name = "terraform",
                      image = "hashicorp/terraform:0.9.8",
                      tty = Option(true),
                      command = Option(Seq("cat")))
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
