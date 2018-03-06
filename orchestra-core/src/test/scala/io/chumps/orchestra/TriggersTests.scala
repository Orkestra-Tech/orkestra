package io.chumps.orchestra

import org.scalatest.Matchers._

import io.chumps.orchestra.job.JobRunners
import io.chumps.orchestra.kubernetes.Jobs
import io.chumps.orchestra.utils.DummyJobs._
import io.chumps.orchestra.utils._

class TriggersTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Triggers {

  scenario("Trigger a job") {
    emptyJobRunner.trigger().futureValue
    val runningJobs = CommonApiServer().runningJobs().futureValue
    runningJobs should have size 1
  }

  scenario("Run a job") {
    val run = emptyJobRunner.run()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    JobRunners.succeedJob(orchestraConfig.runInfo, ()).futureValue
    Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    val runningJobs2 = CommonApiServer().runningJobs().futureValue
    runningJobs2 should have size 0
  }
}
