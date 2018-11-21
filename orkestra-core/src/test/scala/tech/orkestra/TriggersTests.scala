package tech.orkestra

import org.scalatest.Matchers._
import tech.orkestra.Dsl._
import tech.orkestra.job.Jobs
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils._
import org.scalatest.concurrent.Eventually
import shapeless._

class TriggersTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Triggers
    with Eventually {

  scenario("Trigger a job with empty parameter") {
    emptyJob.trigger(HNil).unsafeRunSync()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with empty parameter") {
    val run = emptyJob.run(HNil)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.unsafeRunSync()
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }

  scenario("Trigger a job with 1 parameter") {
    oneParamJob.trigger("someString" :: HNil).unsafeRunSync()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with 1 parameter") {
    val run = oneParamJob.run("someString" :: HNil)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.unsafeRunSync()
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }

  scenario("Trigger a job with multiple parameters") {
    twoParamsJob.trigger("someString" :: true :: HNil).unsafeRunSync()
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("Run a job with multiple parameters") {
    val run = twoParamsJob.run("someString" :: true :: HNil)
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue
    kubernetes.Jobs.delete(orkestraConfig.runInfo).futureValue
    run.unsafeRunSync()
    eventually {
      val runningJobs2 = CommonApiServer().runningJobs().futureValue
      (runningJobs2 should have).size(0)
    }
  }
}
