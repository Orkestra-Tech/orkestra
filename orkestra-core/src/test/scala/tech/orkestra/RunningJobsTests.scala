package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.scalatest.Matchers._
import shapeless.HNil
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.model.RunId
import tech.orkestra.utils._
import org.scalatest.concurrent.Eventually

import scala.concurrent.ExecutionContext

class RunningJobsTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with ElasticsearchTest
    with Eventually {
  implicit override lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "runningJobs" should "return the triggered job" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      _ <- IO.fromFuture(IO(emptyJob.ApiServer().trigger(RunId.random(), HNil)))
      runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
      _ = (runningJobs should have).size(1)
    } yield ()
  }

  it should "return no running job" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
      _ = (runningJobs should have).size(0)
    } yield ()
  }
}
