package com.drivetribe.orchestra

import io.k8s.api.core.v1.Container
import org.scalatest.Matchers._

import com.drivetribe.orchestra.filesystem.Implicits.workDir
import com.drivetribe.orchestra.utils._

class ShellsTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Shells {

  scenario("Run shell command") {
    val log = sh("echo Hello").futureValue
    log should ===("\nHello")
  }

  scenario("Run shell command in a container") {
    val log = sh("echo Hello", Container("someContainer")).futureValue
    log should ===("\nHello")
  }
}
