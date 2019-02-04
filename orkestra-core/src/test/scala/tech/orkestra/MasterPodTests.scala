package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO}
import org.scalatest.Matchers._
import org.scalatest.OptionValues
import tech.orkestra.kubernetes.MasterPod
import tech.orkestra.utils.{JobRunInfo, _}

import scala.concurrent.ExecutionContext

class MasterPodTests
    extends OrkestraSpec
    with OptionValues
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with JobRunInfo {
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "get" should "return the master pod" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      masterPod <- MasterPod.get[IO]
      _ = masterPod.metadata.value.name.value shouldBe orkestraConfig.podName
    } yield ()
  }
}
