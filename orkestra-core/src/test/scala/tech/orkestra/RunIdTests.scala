package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits._
import tech.orkestra.Dsl._
import tech.orkestra.job.Job
import tech.orkestra.utils._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.JobRunInfo
import org.scalatest.Matchers._
import shapeless.HNil

import scala.concurrent.ExecutionContext

class RunIdTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with ElasticsearchTest
    with JobRunInfo {
  implicit override lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "runId" should "return the RunId" in usingKubernetesClient { implicit kubernetesClient =>
    val job = Job(emptyJobBoard) { _: HNil =>
      IO(runId shouldBe orkestraConfig.runInfo.runId) *> IO.unit
    }

    IO.fromFuture(IO(job.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil))) *>
      job.start(orkestraConfig.runInfo)
  }
}
