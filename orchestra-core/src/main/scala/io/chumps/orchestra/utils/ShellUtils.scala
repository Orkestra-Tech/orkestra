package io.chumps.orchestra.utils

import scala.concurrent.duration._
import scala.concurrent.Await
import io.k8s.api.core.v1.Container

import io.chumps.orchestra.filesystem.Directory

trait ShellUtils {

  def sh(script: String)(implicit workDir: Directory): String =
    Await.result(AsyncShellUtils.sh(script), Duration.Inf)

  def sh(script: String, container: Container)(implicit workDir: Directory): String =
    Await.result(AsyncShellUtils.sh(script, container), Duration.Inf)
}

object ShellUtils extends ShellUtils
