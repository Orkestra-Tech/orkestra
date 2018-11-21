package tech.orkestra

import cats.effect.IO
import tech.orkestra.Dsl._
import tech.orkestra.job.Job
import tech.orkestra.utils._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.JobRunInfo
import org.scalatest.Matchers._
import shapeless.HNil

class RunIdTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with JobRunInfo {

  scenario("Getting the RunId") {
    val job = Job(emptyJobBoard) { _: HNil =>
      IO.pure(runId should ===(orkestraConfig.runInfo.runId)).map(_ => ())
    }

    job.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
    job.start(orkestraConfig.runInfo).futureValue
  }
}
