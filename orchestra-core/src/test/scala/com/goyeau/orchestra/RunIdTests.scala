package com.goyeau.orchestra

import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.job.Job
import com.goyeau.orchestra.utils.DummyJobs._
import com.goyeau.orchestra.utils._
import org.scalatest.Matchers._
import shapeless.HNil

class RunIdTests extends OrchestraSpec with OrchestraConfigTest with KubernetesTest with ElasticsearchTest {

  scenario("Running a job with RunId") {
    val job = Job(emptyWithRunIdJobBoard) { implicit workDir => runId =>
      runId should ===(orchestraConfig.runInfo.runId)
    }

    job.ApiServer().trigger(orchestraConfig.runInfo.runId, orchestraConfig.runInfo.runId :: HNil).futureValue
    job.start(orchestraConfig.runInfo).futureValue
  }

  scenario("Running a job with RunId and 1 parameter") {
    val job = Job(runIdWithOneParamJobBoard) { implicit workDir => (runId, _) =>
      runId should ===(orchestraConfig.runInfo.runId)
    }

    job
      .ApiServer()
      .trigger(orchestraConfig.runInfo.runId, orchestraConfig.runInfo.runId :: "someString" :: HNil)
      .futureValue
    job.start(orchestraConfig.runInfo).futureValue
  }

  scenario("Running a job with 1 parameter and RunId") {
    val job = Job(oneParamWithRunIdJobBoard) { implicit workDir => (_, runId) =>
      runId should ===(orchestraConfig.runInfo.runId)
    }

    job
      .ApiServer()
      .trigger(orchestraConfig.runInfo.runId, "someString" :: orchestraConfig.runInfo.runId :: HNil)
      .futureValue
    job.start(orchestraConfig.runInfo).futureValue
  }
}
