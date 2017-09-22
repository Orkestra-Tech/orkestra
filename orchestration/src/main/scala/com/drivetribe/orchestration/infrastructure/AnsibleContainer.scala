package com.drivetribe.orchestration.infrastructure

import com.drivetribe.orchestration.Environment
import com.goyeau.orchestra._
import com.goyeau.orchestra.filesystem.Directory
import com.typesafe.scalalogging.LazyLogging
import io.k8s.api.core.v1.Container

object AnsibleContainer
    extends Container(name = "ansible",
                      image = "registry.drivetribe.com/tools/ansible:cached",
                      tty = Option(true),
                      command = Option(Seq("cat")))
    with LazyLogging {

  def install()(implicit workDir: Directory) = {
    logger.info("Install Ansible dependencies")
    sh("ansible-galaxy install -r ansible/requirements.yml", this)
  }

  def init(environment: Environment)(implicit workDir: Directory) = {
    logger.info("Init Ansible")
    sh(
      s"""ansible-playbook init.yml \\
         |  --vault-password-file /opt/docker/secrets/ansible/vault-pass \\
         |  --private-key /opt/docker/secrets/ssh/drivetribe.pem \\
         |  -e env_name=${environment.entryName}""".stripMargin,
      this
    )
  }

  def playbook(playbook: String, params: String = "")(implicit workDir: Directory) =
    sh(
      s"""ansible-playbook $playbook \\
         |  --vault-password-file /opt/docker/secrets/ansible/vault-pass \\
         |  --private-key /opt/docker/secrets/ssh/drivetribe.pem \\
         |  $params""".stripMargin,
      this
    )
}
