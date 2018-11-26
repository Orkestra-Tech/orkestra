package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import io.k8s.api.core.v1.Container
import org.scalatest.Matchers._
import tech.orkestra.filesystem.Implicits.workDir
import tech.orkestra.utils._

import scala.concurrent.ExecutionContext

class ShellsTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with ElasticsearchTest
    with Shells[IO] {
  implicit lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "sh" should "Run shell command" in {
    val log = sh("echo Hello").unsafeRunSync()
    log should ===("\nHello")
  }

  it should "Run shell command in a container" in {
    val log = sh("echo Hello", Container("someContainer")).unsafeRunSync()
    log should ===("\nHello")
  }
}
