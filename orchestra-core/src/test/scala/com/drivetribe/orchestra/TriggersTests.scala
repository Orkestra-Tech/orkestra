package com.drivetribe.orchestra

import org.scalatest.Matchers._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.job.Jobs
import com.drivetribe.orchestra.utils.DummyJobs._
import com.drivetribe.orchestra.utils._
import org.scalatest.concurrent.Eventually

class TriggersTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Triggers
    with Eventually {

  scenario("Trigger a job with empty parameter") {
    emptyJob.trigger().futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with empty parameter") {
    val run = emptyJob.run()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    Jobs.succeedJob(orchestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }

  scenario("Trigger a job with 1 parameter") {
    oneParamJob.trigger("someString").futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with 1 parameter") {
    val run = oneParamJob.run("someString")
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    Jobs.succeedJob(orchestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }

  scenario("Trigger a job with multiple parameters") {
    twoParamsJob.trigger("someString", true).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }
  }

  scenario("Run a job with multiple parameters") {
    val run = twoParamsJob.run("someString", true)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      runningJobs should have size 1
    }

    Jobs.succeedJob(orchestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orchestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      runningJobs2 should have size 0
    }
  }
}
