package io.chumps.orchestra

import java.io.PrintStream

import scala.concurrent.Future

import io.k8s.api.core.v1.Container
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import shapeless.HNil

import io.chumps.orchestra.filesystem.Implicits.workDir
import io.chumps.orchestra.job.JobRunners
import io.chumps.orchestra.model.{Page, RunId}
import io.chumps.orchestra.utils._
import io.chumps.orchestra.utils.AkkaImplicits._

class LoggingTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with AsyncShells
    with Stages {

  scenario("Log stuff and get it back") {
    val message = "Log stuff and get it back"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId), true)
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

    val stageName = "Log stuff in a stage and get it back"
    val message = "Log stuff in a stage and get it back"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId), true)
    ) {
      stage(stageName) {
        println(message)
      }
    }

    eventually {
      val logs = CommonApiServer().logs(orchestraConfig.runInfo.runId, Page(None, 10000)).futureValue
      logs should have size 2

      logs.headOption.value.line should ===(s"Stage: $stageName")
      logs.headOption.value.stageName should ===(Some(stageName))
      logs(1).line should ===(message)
      logs(1).stageName should ===(Some(stageName))
    }
  }

  scenario("Log stuff in parallel stages and get it back") {
    DummyJobs.emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue

    val stage1Name = "Log stuff in parallel stages and get it back 1"
    val stage2Name = "Log stuff in parallel stages and get it back 2"
    val message1 = "Log stuff in parallel stages and get it back 1"
    val message2 = "Log stuff in parallel stages and get it back 2"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId), true)
    ) {
      Future
        .sequence(
          Seq(
            Future(stage(stage1Name) {
              println(message1)
            }),
            Future(stage(stage2Name) {
              println(message2)
            })
          )
        )
        .futureValue
    }

    eventually {
      val logs = CommonApiServer().logs(orchestraConfig.runInfo.runId, Page(None, 10000)).futureValue
      logs should have size 4

      logs.map(_.line) should contain(s"Stage: $stage1Name")
      logs.map(_.line) should contain(message1)
      logs.map(_.line) should contain(s"Stage: $stage2Name")
      logs.map(_.line) should contain(message2)

      val stage1Log = logs.filter(_.stageName == Option(stage1Name))
      stage1Log should have size 2
      val stage2Log = logs.filter(_.stageName == Option(stage2Name))
      stage2Log should have size 2
    }
  }

  scenario("Log stuff with a shell command in a container and in a stage and it back") {
    DummyJobs.emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue

    val stageName = "Log stuff with a shell command in a container and in a stage and it back"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId), true)
    ) {
      stage(stageName) {
        sh("echo Hello", Container("someContainer")).futureValue
      }
    }

    eventually {
      val logs = CommonApiServer().logs(orchestraConfig.runInfo.runId, Page(None, 10000)).futureValue
      logs.headOption.value.line should ===(s"Stage: $stageName")
      logs.headOption.value.stageName should ===(Some(stageName))
      logs(1).stageName should ===(Some(stageName))
    }
  }
}
