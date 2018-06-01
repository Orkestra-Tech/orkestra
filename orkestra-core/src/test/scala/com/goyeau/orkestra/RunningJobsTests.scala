package com.goyeau.orkestra

import org.scalatest.Matchers._
import shapeless.HNil
import com.goyeau.orkestra.utils.DummyJobs._
import com.goyeau.orkestra.model.RunId
import com.goyeau.orkestra.utils._
import org.scalatest.concurrent.Eventually

class RunningJobsTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Eventually {

  scenario("Trigger a job") {
    emptyJob.ApiServer().trigger(RunId.random(), HNil).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }
  }

  scenario("No running job") {
    val runningJobs = CommonApiServer().runningJobs().futureValue
    (runningJobs should have).size(0)
  }
}
