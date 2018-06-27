package tech.orkestra

import tech.orkestra.model.RunId
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils._
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import shapeless.HNil

class StopJobTests
    extends OrkestraSpec
    with OrkestraConfigTest
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
