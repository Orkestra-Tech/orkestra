package com.goyeau.orchestra

import com.goyeau.orchestra.model.RunId
import com.goyeau.orchestra.utils.DummyJobs._
import com.goyeau.orchestra.utils._
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import shapeless.HNil

class StopJobTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Eventually {

  scenario("Stop a job") {
    val runId = RunId.random()
    emptyJob.ApiServer().trigger(runId, HNil).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(1)
    }

    emptyJob.ApiServer().stop(runId).futureValue
    eventually {
      val runningJobs = CommonApiServer().runningJobs().futureValue
      (runningJobs should have).size(0)
    }
  }
}
