package com.goyeau.orkestra

import org.scalatest.Matchers._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.job.Jobs
import com.goyeau.orkestra.utils.DummyJobs._
import com.goyeau.orkestra.utils._
import org.scalatest.concurrent.Eventually

class TriggersTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Triggers
    with Eventually {

  scenario("Trigger a job with empty parameter") {
    emptyJob.trigger().futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with empty parameter") {
    val run = emptyJob.run()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }

  scenario("Trigger a job with 1 parameter") {
    oneParamJob.trigger("someString").futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with 1 parameter") {
    val run = oneParamJob.run("someString")
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }

  scenario("Trigger a job with multiple parameters") {
    twoParamsJob.trigger("someString", true).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with multiple parameters") {
    val run = twoParamsJob.run("someString", true)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.futureValue
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }
}
