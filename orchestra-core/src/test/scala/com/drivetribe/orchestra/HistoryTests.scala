package com.drivetribe.orchestra

import java.io.PrintStream

import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import shapeless.HNil

import com.drivetribe.orchestra.job.JobRunners
import com.drivetribe.orchestra.model.Page
import com.drivetribe.orchestra.utils._
import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.drivetribe.orchestra.utils.DummyJobs._

class HistoryTests
    extends OrchestraSpec
    with OrchestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Stages {

  scenario("Job triggered") {
    val tags = Seq("firstTag", "secondTag")
    emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil, tags).futureValue

    eventually {
      val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
      history.runs should have size 1
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orchestraConfig.runInfo)
      run.tags should ===(tags)
      run.latestUpdateOn should ===(run.triggeredOn)
      run.result should ===(None)
    }
  }

  scenario("Job running") {
    emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue
    JobRunners.pong(orchestraConfig.runInfo)

    eventually {
      val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
      history.runs should have size 1
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orchestraConfig.runInfo)
      run.latestUpdateOn should not be run.triggeredOn
      run.result should ===(None)
    }
  }

  scenario("Job succeded") {
    emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue
    JobRunners.succeedJob(orchestraConfig.runInfo, ()).futureValue

    eventually {
      val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
      history.runs should have size 1
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orchestraConfig.runInfo)
      run.result should ===(Some(Right(())))
    }
  }

  scenario("Job failed") {
    emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue
    val exceptionMessage = "Oh my god"
    JobRunners
      .failJob(orchestraConfig.runInfo, new Exception(exceptionMessage))
      .recover { case _ => () }
      .futureValue

    eventually {
      val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
      history.runs should have size 1
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orchestraConfig.runInfo)
      run.result.value.left.toOption.value.getMessage should ===(exceptionMessage)
    }
  }

  scenario("No history for never triggered job") {
    val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
    history.runs shouldBe empty
  }

  scenario("History contains stages") {
    emptyJobRunner.ApiServer().trigger(orchestraConfig.runInfo.runId, HNil).futureValue

    val stageName = "Testing"
    JobRunners.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orchestraConfig.runInfo.runId), true)
    ) {
      stage(stageName) {
        println("Hello")
      }
    }

    eventually {
      val history = emptyJobRunner.ApiServer().history(Page(None, -50)).futureValue
      history.runs should have size 1
      history.runs.headOption.value._2 should have size 1
      history.runs.headOption.value._2.headOption.value.name should ===(stageName)
    }
  }
}
