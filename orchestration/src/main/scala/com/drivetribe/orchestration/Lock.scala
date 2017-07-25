package com.drivetribe.orchestration

import java.io.{File, RandomAccessFile}

import com.goyeau.orchestra.Config

object Lock {

  private def deploymentChannel(environment: Environment, project: String) = {
    val lockFile = new File(s"${Config.home}/locks/${environment.entryName.toLowerCase}/$project/deployment")
    lockFile.getParentFile.mkdirs()
    new RandomAccessFile(lockFile, "rw").getChannel
  }

  def onDeployment[T](environment: Environment, project: String)(f: => T): T = {
    val channel = deploymentChannel(environment, project)
    channel.lock() // Blocking
    try f
    finally channel.close()
  }

  def onEnvironment[T](environment: Environment)(f: => T): T =
    onDeployment(environment, "backend") {
      onDeployment(environment, "frontend") {
        onDeployment(environment, "studio")(f)
      }
    }
}
