package tech.orkestra

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import io.circe.shapes._
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually

import scala.concurrent.ExecutionContext
import shapeless._
import tech.orkestra.job.Jobs
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils._
import scala.concurrent.duration._

class TriggersTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest[IO]
    with ElasticsearchTest
    with Triggers[IO]
    with Eventually {
  implicit lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  "Trigger a job" should "start a job given empty parameter" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      _ <- emptyJob.trigger(HNil)
      _ <- refreshIndexes
      runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
      _ = (runningJobs should have).size(1)
    } yield ()
  }

  it should "start a job given 1 parameter" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      _ <- oneParamJob.trigger("someString" :: HNil)
      _ <- refreshIndexes
      runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
      _ = (runningJobs should have).size(1)
    } yield ()
  }

  it should "start a job given multiple parameters" in usingKubernetesClient { implicit kubernetesClient =>
    for {
      _ <- twoParamsJob.trigger("someString" :: true :: HNil)
      _ <- refreshIndexes
      runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
      _ = (runningJobs should have).size(1)
    } yield ()
  }

  "Run a job" should "start a job and await result given empty parameter" in usingKubernetesClient {
    implicit kubernetesClient =>
      for {
        run <- emptyJob.run(HNil).start
        _ <- timer.sleep(1.milli)
        _ <- refreshIndexes
        runningJobs <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
        _ = (runningJobs should have).size(1)

        _ <- Jobs.succeedJob(orkestraConfig.runInfo, ())
        _ <- kubernetes.Jobs.delete(orkestraConfig.runInfo)
        _ <- run.join
        _ <- refreshIndexes
        runningJobs2 <- IO.fromFuture(IO(CommonApiServer().runningJobs()))
        _ = (runningJobs2 should have).size(0)
      } yield ()
  }

//  it should "start a job and await result given 1 parameter" in usingKubernetesClient { implicit kubernetesClient =>
//    for {
//      run <- oneParamJob.run("someString" :: HNil).start
//      _ = eventually {
//        val runningJobs = CommonApiServer().runningJobs().futureValue
//        (runningJobs should have).size(1)
//      }
//
//      _ <- IO.fromFuture(IO(Jobs.succeedJob(orkestraConfig.runInfo, ())))
//      _ <- kubernetes.Jobs.delete(orkestraConfig.runInfo)
//      _ <- run.join
//      _ = eventually {
//        val runningJobs2 = CommonApiServer().runningJobs().futureValue
//        (runningJobs2 should have).size(0)
//      }
//    } yield ()
//  }
//
//  it should "start a job and await result given multiple parameters" in usingKubernetesClient {
//    implicit kubernetesClient =>
//      for {
//        run <- twoParamsJob.run("someString" :: true :: HNil).start
//        _ = eventually {
//          val runningJobs = CommonApiServer().runningJobs().futureValue
//          (runningJobs should have).size(1)
//        }
//
//        _ <- IO.fromFuture(IO(Jobs.succeedJob(orkestraConfig.runInfo, ())))
//        _ <- kubernetes.Jobs.delete(orkestraConfig.runInfo)
//        _ <- run.join
//        _ = eventually {
//          val runningJobs2 = CommonApiServer().runningJobs().futureValue
//          (runningJobs2 should have).size(0)
//        }
//      } yield ()
//  }
}
