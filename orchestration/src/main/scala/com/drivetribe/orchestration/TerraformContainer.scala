package com.drivetribe.orchestration

import com.goyeau.orchestra.io.Directory
import com.goyeau.orchestra.kubernetes._

object TerraformContainer extends Container("terraform", "hashicorp/terraform:0.9.8", tty = true, Seq("cat")) {

  def rootDir(environment: Environment) = s"terraform/providers/aws/app/${environment.environmentType.entryName}"

  def init(remoteState: String)(implicit workDir: Directory) = {
    println("Init Terraform")
    sh(s"terraform init -backend-config=key=tfstates/$remoteState.tfstate", this)
  }

  def apply(params: String = "")(implicit workDir: Directory) =
    sh(s"terraform apply $params", this)

  def destroy(params: String = "")(implicit workDir: Directory) =
    sh(s"terraform destroy -force $params", this)
}
