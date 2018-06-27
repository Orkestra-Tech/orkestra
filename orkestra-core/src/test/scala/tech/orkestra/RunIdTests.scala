package tech.orkestra

import tech.orkestra.Dsl._
import tech.orkestra.job.Job
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils._
import org.scalatest.Matchers._
import shapeless.HNil

class RunIdTests extends OrkestraSpec with OrkestraConfigTest with KubernetesTest with ElasticsearchTest {

  scenario("Running a job with RunId") {
    val job = Job(emptyWithRunIdJobBoard) { implicit workDir => runId =>
      runId should ===(orkestraConfig.runInfo.runId)
    }

    job.ApiServer().trigger(orkestraConfig.runInfo.runId, orkestraConfig.runInfo.runId :: HNil).futureValue
    job.start(orkestraConfig.runInfo).futureValue
  }

  scenario("Running a job with RunId and 1 parameter") {
    val job = Job(runIdWithOneParamJobBoard) { implicit workDir => (runId, _) =>
      runId should ===(orkestraConfig.runInfo.runId)
    }

    job
      .ApiServer()
      .trigger(orkestraConfig.runInfo.runId, orkestraConfig.runInfo.runId :: "someString" :: HNil)
      .futureValue
    job.start(orkestraConfig.runInfo).futureValue
  }

  scenario("Running a job with 1 parameter and RunId") {
    val job = Job(oneParamWithRunIdJobBoard) { implicit workDir => (_, runId) =>
      runId should ===(orkestraConfig.runInfo.runId)
    }

    job
      .ApiServer()
      .trigger(orkestraConfig.runInfo.runId, "someString" :: orkestraConfig.runInfo.runId :: HNil)
      .futureValue
    job.start(orkestraConfig.runInfo).futureValue
  }
}
