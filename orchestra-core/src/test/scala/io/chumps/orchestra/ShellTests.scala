package io.chumps.orchestra

import io.k8s.api.core.v1.Container
import org.scalatest.Matchers._

import io.chumps.orchestra.filesystem.Implicits.workDir
import io.chumps.orchestra.utils.{AsyncShellUtils, KubernetesTest, OrchestraConfigTest, OrchestraSpec}

class ShellTests extends OrchestraSpec with OrchestraConfigTest with KubernetesTest with AsyncShellUtils {

  scenario("Run shell command") {
    val log = sh("echo Hello").futureValue
    log should ===("\nHello")
  }

  scenario("Run shell command in container") {
    val log = sh("echo Hello", Container("someContainer")).futureValue
    log should ===("\nHello")
  }
}
