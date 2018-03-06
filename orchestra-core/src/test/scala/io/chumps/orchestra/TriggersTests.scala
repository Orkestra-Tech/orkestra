package io.chumps.orchestra

import org.scalatest.Matchers._

import io.chumps.orchestra.Dsl._
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

  scenario("Trigger a job with empty parameter") {
    emptyJobRunner.trigger().futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with empty parameter") {
    val run = emptyJobRunner.run()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    JobRunners.succeedJob(orchestraConfig.runInfo, ()).futureValue
    Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }

  scenario("Trigger a job with 1 parameter") {
    oneParamJobRunner.trigger("someString").futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with 1 parameter") {
    val run = oneParamJobRunner.run("someString")
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    JobRunners.succeedJob(orchestraConfig.runInfo, ()).futureValue
    Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }

  scenario("Trigger a job with multiple parameters") {
    twoParamsJobRunner.trigger("someString", true).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with multiple parameters") {
    val run = twoParamsJobRunner.run("someString", true)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    JobRunners.succeedJob(orchestraConfig.runInfo, ()).futureValue
    Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }
}
