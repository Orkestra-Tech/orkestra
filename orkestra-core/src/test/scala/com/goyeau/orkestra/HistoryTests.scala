package com.goyeau.orkestra

import java.io.PrintStream

import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import shapeless.HNil
import com.goyeau.orkestra.job.Jobs
import com.goyeau.orkestra.model.Page
import com.goyeau.orkestra.utils._
import com.goyeau.orkestra.utils.AkkaImplicits._
import com.goyeau.orkestra.utils.DummyJobs._
import org.scalatest.concurrent.Eventually

class HistoryTests
    extends OrkestraSpec
    with OrkestraConfigTest
    with KubernetesTest
    with ElasticsearchTest
    with Stages
    with Eventually {

  scenario("Job triggered") {
    val tags = Seq("firstTag", "secondTag")
    emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil, tags).futureValue

    eventually {
      val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
      (history.runs should have).size(1)
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orkestraConfig.runInfo)
      run.tags should ===(tags)
      run.latestUpdateOn should ===(run.triggeredOn)
      run.result should ===(None)
    }
  }

  scenario("Job running") {
    emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
    Jobs.pong(orkestraConfig.runInfo)

    eventually {
      val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
      (history.runs should have).size(1)
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orkestraConfig.runInfo)
      run.latestUpdateOn should not be run.triggeredOn
      run.result should ===(None)
    }
  }

  scenario("Job succeeded") {
    emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
    Jobs.succeedJob(orkestraConfig.runInfo, ()).futureValue

    eventually {
      val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
      (history.runs should have).size(1)
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orkestraConfig.runInfo)
      run.result should ===(Some(Right(())))
    }
  }

  scenario("Job failed") {
    emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
    val exceptionMessage = "Oh my god"
    Jobs
      .failJob(orkestraConfig.runInfo, new Exception(exceptionMessage))
      .recover { case _ => () }
      .futureValue

    eventually {
      val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
      (history.runs should have).size(1)
      val run = history.runs.headOption.value._1
      run.runInfo should ===(orkestraConfig.runInfo)
      run.result.value.left.toOption.value.getMessage should ===(exceptionMessage)
    }
  }

  scenario("No history for never triggered job") {
    val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
    history.runs shouldBe empty
  }

  scenario("History contains stages") {
    emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue

    val stageName = "Testing"
    Jobs.withOutErr(
      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orkestraConfig.runInfo.runId), true)
    ) {
      stage(stageName) {
        println("Hello")
      }
    }

    eventually {
      val history = emptyJob.ApiServer().history(Page(None, -50)).futureValue
      (history.runs should have).size(1)
      (history.runs.headOption.value._2 should have).size(1)
      history.runs.headOption.value._2.headOption.value.name should ===(stageName)
    }
  }
}
