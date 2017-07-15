package com.goyeau.orchestration

import com.goyeau.orchestra.io.Directory
import com.goyeau.orchestra.kubernetes._

object TerraformContainer extends Container("terraform", "hashicorp/terraform:0.9.8", tty = true, Seq("cat")) {

  def init(environment: Environment)(implicit workDir: Directory) = {
    println("Init Terraform")
    s"terraform init -backend-config=key=tfstates/app-${environment.entryName}.tfstate" !> this
  }

  def apply(params: String = "") = s"terraform apply $params" !> this
}
