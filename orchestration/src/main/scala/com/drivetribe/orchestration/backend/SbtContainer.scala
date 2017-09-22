package com.drivetribe.orchestration.backend

import com.goyeau.orchestra._
import com.goyeau.orchestra.filesystem.Directory
import io.k8s.api.core.v1.{Container, VolumeMount}

object SbtContainer
    extends Container(
      name = "sbt",
      image = "registry.drivetribe.com/tools/sbt:cached",
      tty = Option(true),
      command = Option(Seq("cat")),
      volumeMounts = Option(Seq(VolumeMount("docker-sock", mountPath = "/var/run/docker.sock")))
    ) {

  def apply(command: String)(implicit workDir: Directory) = sh(s"sbt -mem 2048 $command", this)
}
