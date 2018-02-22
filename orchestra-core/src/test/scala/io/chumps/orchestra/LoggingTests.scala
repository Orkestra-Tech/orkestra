package io.chumps.orchestra

import java.io.PrintStream

import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import shapeless.HNil

import io.chumps.orchestra.job.JobRunners
import io.chumps.orchestra.model.{Page, RunId}
import io.chumps.orchestra.utils._

class LoggingTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with StageUtils {

  scenario("Log stuff and get it back") {
    val message = "Hello"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId))
    ) {
      println(message)
    }

    eventually {
      val logs = CommonApiServer().logs(orchestraConfig.runInfo.runId, Page(None, 10000)).futureValue
      logs should have size 1
      logs.headOption.value.line should ===(message)
    }
  }

  scenario("No logs for run that logged nothing") {
    val logs = CommonApiServer().logs(RunId.random(), Page(None, 10000)).futureValue
    logs shouldBe empty
  }

  scenario("Log stuff in a stage and get it back") {
    DummyJobs.emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue

    val stageName = "Testing"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId))
    ) {
      stage(stageName) {
        println("Hello")
      }
    }

    eventually {
      val logs = CommonApiServer().logs(orchestraConfig.runInfo.runId, Page(None, 10000)).futureValue
      logs should have size 2
      logs.headOption.value.line should ===(s"Stage: $stageName")
      logs.headOption.value.stageName should ===(Some(stageName))
      logs(1).line should ===("Hello")
      logs(1).stageName should ===(Some(stageName))
    }
  }
}
