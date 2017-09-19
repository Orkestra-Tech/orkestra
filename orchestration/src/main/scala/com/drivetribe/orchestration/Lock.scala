package com.drivetribe.orchestration

import java.io.RandomAccessFile
import java.nio.file.Paths

import com.goyeau.orchestra.OrchestraConfig

object Lock {

  def lock[T](id: String)(f: => T): T = {
    val lockFile = Paths.get(s"${OrchestraConfig.home}/locks", id).toFile
    lockFile.getParentFile.mkdirs()
    val channel = new RandomAccessFile(lockFile, "rw").getChannel

    channel.lock() // Blocking
    try f
    finally channel.close()
  }

  def onDeployment[T](environment: Environment, project: Project)(f: => T): T =
    lock(s"${environment.entryName.toLowerCase}/$project/deployment")(f)

  def onEnvironment[T](environment: Environment)(f: => T): T =
    onDeployment(environment, Project.Backend) {
      onDeployment(environment, Project.Frontend) {
        onDeployment(environment, Project.Studio)(f)
      }
    }

  def onCheckpoint[T](environment: Environment)(f: => T): T =
    lock(environment.entryName.toLowerCase)(f)
}

trait Project
object Project {
  case object Backend extends Project
  case object Frontend extends Project
  case object Studio extends Project
}
