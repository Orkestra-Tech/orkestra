package io.chumps.orchestra

import io.circe.shapes._
import org.scalatest.Matchers._
import shapeless.HNil

import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.{JobId, RunId}
import io.chumps.orchestra.utils._

class RunningJobsTests extends OrchestraSpec with OrchestraConfigTest with KubernetesTest with ElasticsearchTest {

  scenario("Trigger a job") {
    lazy val job = board.Job[() => Unit](JobId("someJob"), "Some Job")()
    lazy val jobRunner = JobRunner(job) { implicit workDir => () =>
      println("Done")
    }

    jobRunner.ApiServer().trigger(RunId.random(), HNil).futureValue
    val runningJobs = CommonApiServer().runningJobs().futureValue
    runningJobs should have size 1
  }

  scenario("No running job") {
    val runningJobs = CommonApiServer().runningJobs().futureValue
    runningJobs should have size 0
  }
}
