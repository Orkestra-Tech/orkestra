package tech.orkestra

import tech.orkestra.Dsl._
import tech.orkestra.job.Job
import tech.orkestra.utils._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.JobRunInfo
import org.scalatest.Matchers._
import shapeless.HNil

class RunIdTests extends OrkestraSpec with OrkestraConfigTest with KubernetesTest with ElasticsearchTest with JobRunInfo {

  scenario("Getting the RunId") {
    val job = Job(emptyJobBoard) { implicit workDir => () =>
      runId should ===(orkestraConfig.runInfo.runId)
    }

    job.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
    job.start(orkestraConfig.runInfo).futureValue
  }
}
