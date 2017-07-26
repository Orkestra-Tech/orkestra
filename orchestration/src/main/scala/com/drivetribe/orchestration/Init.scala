package com.drivetribe.orchestration

import com.goyeau.orchestra._
import com.goyeau.orchestra.filesystem.Directory

object Init {

  def apply(environment: Environment, ansible: AnsibleContainer.type, terraform: TerraformContainer.type)(
    implicit workDir: Directory
  ) =
    dir(terraform.rootDir(environment)) { implicit workDir =>
      terraform.init(s"app-${environment.entryName}")
      ansible.init(environment)
    }
}
