package com.drivetribe.orchestration

import java.io.{File, RandomAccessFile}

import com.goyeau.orchestra.OrchestraConfig

object Lock {

  private def deploymentChannel(environment: Environment, project: Project) = {
    val lockFile = new File(s"${OrchestraConfig.home}/locks/${environment.entryName.toLowerCase}/$project/deployment")
    lockFile.getParentFile.mkdirs()
    new RandomAccessFile(lockFile, "rw").getChannel
  }

  def onDeployment[T](environment: Environment, project: Project)(f: => T): T = {
    val channel = deploymentChannel(environment, project)
    channel.lock() // Blocking
    try f
    finally channel.close()
  }

  def onEnvironment[T](environment: Environment)(f: => T): T =
    onDeployment(environment, Project.Backend) {
      onDeployment(environment, Project.Frontend) {
        onDeployment(environment, Project.Studio)(f)
      }
    }
}

trait Project
object Project {
  case object Backend extends Project
  case object Frontend extends Project
  case object Studio extends Project
}
