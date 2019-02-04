package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import tech.orkestra.model.RunId
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils._
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import shapeless.HNil

import scala.concurrent.ExecutionContext

class StopJobTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with ElasticsearchTest
    with Eventually {
  implicit override lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "stop" should "Stop a job" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      runId <- IO.pure(RunId.random())
      _ = emptyJob.ApiServer().trigger(runId, HNil).futureValue
      _ = eventually {
        val runningJobs = CommonApiServer().runningJobs().futureValue
        (runningJobs should have).size(1)
      }

      _ = emptyJob.ApiServer().stop(runId).futureValue
      _ = eventually {
        val runningJobs = CommonApiServer().runningJobs().futureValue
        (runningJobs should have).size(0)
      }
    } yield ()
  }
}
